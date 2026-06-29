/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.parser.CsvParser;
import org.gensokyo.data.calcite.parser.DefaultCsvParser;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV row source that reads file lines incrementally in bounded chunks.
 * <p>
 * Call {@link #hasNextChunk()} and {@link #nextChunk(int)} to read data. {@link #rows()} is intentionally
 * empty; use the chunked API instead of materializing all rows.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
public class ChunkedCsvRowSource implements ChunkedRowSource {

    /** Default chunk size for CSV when policy does not override {@code sourceChunkSize} (D-03). */
    public static final int DEFAULT_CSV_CHUNK_SIZE = 1_000;

    private final String name;
    private final CsvSourceVO source;
    private final CsvParser csvParser;
    private final int defaultChunkSize;

    private RowSchema schema;
    private List<String> columnNames;
    private long rowsReadSoFar;
    private boolean exhausted;
    private boolean readerOpen;
    private boolean headerConsumed;
    private long maxRowsCap;

    private BufferedReader reader;
    private int physicalLineNumber;

    /**
     * Opens a chunked CSV reader for the given source configuration.
     *
     * @param name             logical source name
     * @param source           CSV source configuration
     * @param csvParser        line parser (UTF-8 with optional BOM)
     * @param defaultChunkSize default rows per chunk when callers pass policy chunk size
     */
    public ChunkedCsvRowSource(String name, CsvSourceVO source, CsvParser csvParser, int defaultChunkSize) {
        this.name = name;
        this.source = source;
        this.csvParser = csvParser;
        this.defaultChunkSize = defaultChunkSize > 0 ? defaultChunkSize : DEFAULT_CSV_CHUNK_SIZE;
        this.maxRowsCap = source.getMaxRows() == null ? Long.MAX_VALUE : source.getMaxRows();
        this.physicalLineNumber = 0;
    }

    /**
     * Convenience constructor using {@link #DEFAULT_CSV_CHUNK_SIZE}.
     *
     * @param name      logical source name
     * @param source    CSV source configuration
     * @param csvParser line parser
     */
    public ChunkedCsvRowSource(String name, CsvSourceVO source, CsvParser csvParser) {
        this(name, source, csvParser, DEFAULT_CSV_CHUNK_SIZE);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RowSchema schema() {
        if (schema != null) {
            return schema;
        }
        ensureReaderOpen();
        return schema != null ? schema : new RowSchema();
    }

    /**
     * Does not materialize rows. Use {@link #nextChunk(int)} instead.
     *
     * @return empty unmodifiable list
     */
    @Override
    public List<Row> rows() {
        return List.of();
    }

    @Override
    public boolean supportsChunking() {
        return true;
    }

    @Override
    public boolean hasNextChunk() {
        return !exhausted && !maxRowsCapReached();
    }

    @Override
    public List<Row> nextChunk(int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be positive");
        }
        if (exhausted || maxRowsCapReached()) {
            return List.of();
        }
        ensureReaderOpen();
        int chunkLimit = effectiveChunkLimit(maxRows);
        List<Row> chunk = new ArrayList<>(chunkLimit);
        try {
            while (chunk.size() < chunkLimit && !maxRowsCapReached()) {
                String line = readNextDataLine();
                if (line == null) {
                    exhausted = true;
                    break;
                }
                List<String> values = parseLineValues(line);
                if (values.stream().allMatch(String::isBlank)) {
                    continue;
                }
                chunk.add(toRow(values, physicalLineNumber));
                rowsReadSoFar++;
                if (maxRowsCapReached()) {
                    exhausted = true;
                    break;
                }
            }
        } catch (IOException ex) {
            closeReader();
            throw new IllegalStateException("Failed to read chunk from CSV source [" + name + "]", ex);
        }
        if (exhausted) {
            closeReader();
        }
        return List.copyOf(chunk);
    }

    @Override
    public long rowsReadSoFar() {
        return rowsReadSoFar;
    }

    /**
     * Default chunk size configured for this source (D-03).
     *
     * @return rows per chunk default
     */
    public int defaultChunkSize() {
        return defaultChunkSize;
    }

    private void ensureReaderOpen() {
        if (readerOpen) {
            return;
        }
        if (source.getPath() == null || source.getPath().isBlank()) {
            throw new IllegalArgumentException("CSV source path must not be blank");
        }
        assertUtf8Charset();
        try {
            reader = Files.newBufferedReader(Path.of(source.getPath()), StandardCharsets.UTF_8);
            readerOpen = true;
            consumeHeaderIfNeeded();
        } catch (IOException ex) {
            closeReader();
            throw new IllegalStateException("Failed to open CSV source: " + source.getPath(), ex);
        }
    }

    private void assertUtf8Charset() {
        String charset = source.getCharset();
        if (charset != null && !charset.isBlank()
                && !StandardCharsets.UTF_8.name().equalsIgnoreCase(charset.trim())) {
            throw new IllegalArgumentException(
                    "Chunked CSV source supports UTF-8 with optional BOM only; got charset [" + charset + "]");
        }
    }

    private void consumeHeaderIfNeeded() throws IOException {
        if (headerConsumed) {
            return;
        }
        String firstLine = reader.readLine();
        physicalLineNumber++;
        if (firstLine == null) {
            exhausted = true;
            headerConsumed = true;
            return;
        }
        // Strip UTF-8 BOM from the first physical line when present (D-09).
        firstLine = DefaultCsvParser.stripUtf8Bom(firstLine);
        if (source.isHeader()) {
            List<String> headerValues = parseLineValues(firstLine);
            columnNames = resolveColumnNames(headerValues);
            buildSchemaFromColumns(columnNames);
        } else {
            columnNames = resolveColumnNames(null);
            List<String> values = parseLineValues(firstLine);
            if (!values.stream().allMatch(String::isBlank)) {
                // First line is data — push it back via a one-line replay buffer is awkward;
                // re-parse by treating line as pending: store for next read.
                pendingDataLine = firstLine;
            }
        }
        headerConsumed = true;
    }

    private String pendingDataLine;

    private String readNextDataLine() throws IOException {
        if (pendingDataLine != null) {
            String line = pendingDataLine;
            pendingDataLine = null;
            return line;
        }
        String line = reader.readLine();
        if (line != null) {
            physicalLineNumber++;
        }
        return line;
    }

    private List<String> parseLineValues(String line) {
        if (csvParser instanceof DefaultCsvParser defaultParser) {
            return defaultParser.parseLine(source, line, physicalLineNumber);
        }
        // Fallback for injected test parsers: parse single line via list wrapper.
        return csvParser.parse(source, List.of(line)).getFirst();
    }

    private List<String> resolveColumnNames(List<String> headerValues) {
        if (source.getSchema() != null && source.getSchema().getColumns() != null
                && !source.getSchema().getColumns().isEmpty()) {
            return source.getSchema().getColumns().stream()
                    .map(ColumnDef::getName)
                    .toList();
        }
        if (headerValues != null && !headerValues.isEmpty()) {
            return headerValues;
        }
        return List.of();
    }

    private void buildSchemaFromColumns(List<String> columns) {
        if (source.getSchema() != null) {
            schema = source.getSchema();
            return;
        }
        RowSchema inferred = new RowSchema();
        inferred.setColumns(columns.stream()
                .map(column -> new ColumnDef(column, "VARCHAR", true))
                .toList());
        schema = inferred;
    }

    private Row toRow(List<String> values, int lineNumber) {
        List<String> columns = columnNames;
        if (columns.isEmpty()) {
            columns = generatedColumns(values.size());
            if (schema == null) {
                buildSchemaFromColumns(columns);
            }
        }
        if (source.isStrictColumns() && values.size() != columns.size()) {
            throw new IllegalArgumentException("CSV source row width mismatch at line [" + lineNumber + "] for ["
                    + source.getPath() + "]: expected [" + columns.size() + "] columns but got ["
                    + values.size() + "]");
        }
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            row.put(columns.get(i), i < values.size() ? values.get(i) : null);
        }
        return new Row(row);
    }

    private List<String> generatedColumns(int count) {
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            columns.add("c" + (i + 1));
        }
        return columns;
    }

    private int effectiveChunkLimit(int maxRows) {
        if (maxRowsCap == Long.MAX_VALUE) {
            return maxRows;
        }
        long remaining = maxRowsCap - rowsReadSoFar;
        if (remaining <= 0) {
            return 0;
        }
        return (int) Math.min(maxRows, remaining);
    }

    private boolean maxRowsCapReached() {
        return rowsReadSoFar >= maxRowsCap;
    }

    private void closeReader() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to close CSV reader for source [" + name + "]", ex);
            }
            reader = null;
        }
        readerOpen = false;
    }
}
