/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.parser.DefaultJsonParser;
import org.gensokyo.data.calcite.parser.JsonParser;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.JsonSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JSON row source that reads NDJSON or top-level JSON array files incrementally in bounded chunks.
 * <p>
 * Format detection order (D-08):
 * <ol>
 *   <li>Explicit {@link JsonSourceVO#getFormat()} when set: {@code ndjson} or {@code array}</li>
 *   <li>Otherwise auto-detect from the first non-whitespace byte: {@code [} → array, else NDJSON line mode</li>
 * </ol>
 * Call {@link #hasNextChunk()} and {@link #nextChunk(int)} to read data. {@link #rows()} is intentionally
 * empty; use the chunked API instead of materializing all rows.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
public class ChunkedJsonRowSource implements ChunkedRowSource {

    /** Default chunk size for JSON when policy does not override {@code sourceChunkSize} (D-03). */
    public static final int DEFAULT_JSON_CHUNK_SIZE = 1_000;

    private static final String FORMAT_NDJSON = "ndjson";
    private static final String FORMAT_ARRAY = "array";

    private final String name;
    private final JsonSourceVO source;
    private final JsonParser jsonParser;
    private final int defaultChunkSize;

    private RowSchema schema;
    private long rowsReadSoFar;
    private boolean exhausted;
    private boolean readerOpen;
    private long maxRowsCap;

    private JsonReadFormat readFormat;
    private BufferedReader ndjsonReader;
    private DefaultJsonParser.ArrayElementIterator arrayIterator;
    private long ndjsonLineNumber;

    /**
     * Opens a chunked JSON reader for the given source configuration.
     *
     * @param name             logical source name
     * @param source           JSON source configuration
     * @param jsonParser       parser with streaming helpers
     * @param defaultChunkSize default rows per chunk when callers pass policy chunk size
     */
    public ChunkedJsonRowSource(String name, JsonSourceVO source, JsonParser jsonParser, int defaultChunkSize) {
        this.name = name;
        this.source = source;
        this.jsonParser = jsonParser;
        this.defaultChunkSize = defaultChunkSize > 0 ? defaultChunkSize : DEFAULT_JSON_CHUNK_SIZE;
        this.maxRowsCap = source.getMaxRows() == null ? Long.MAX_VALUE : source.getMaxRows();
        this.ndjsonLineNumber = 0;
    }

    /**
     * Convenience constructor using {@link #DEFAULT_JSON_CHUNK_SIZE}.
     *
     * @param name       logical source name
     * @param source     JSON source configuration
     * @param jsonParser parser with streaming helpers
     */
    public ChunkedJsonRowSource(String name, JsonSourceVO source, JsonParser jsonParser) {
        this(name, source, jsonParser, DEFAULT_JSON_CHUNK_SIZE);
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
        if (source.getSchema() != null) {
            schema = source.getSchema();
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
        while (chunk.size() < chunkLimit && !maxRowsCapReached()) {
            Map<String, Object> values = readNextRowValues();
            if (values == null) {
                exhausted = true;
                break;
            }
            inferSchemaFromRow(values);
            chunk.add(new Row(new LinkedHashMap<>(values)));
            rowsReadSoFar++;
            if (maxRowsCapReached()) {
                exhausted = true;
                break;
            }
        }
        if (exhausted) {
            closeReaders();
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
            throw new IllegalArgumentException("JSON source path must not be blank");
        }
        if (source.getRoot() != null && !source.getRoot().isBlank()) {
            throw new IllegalArgumentException(
                    "Chunked JSON source does not support root selector [" + source.getRoot()
                            + "]; use IN_MEMORY mode or a flattened NDJSON/array file");
        }
        Path path = Path.of(source.getPath());
        readFormat = resolveFormat(path);
        Charset charset = Charset.forName(source.getCharset());
        try {
            if (readFormat == JsonReadFormat.NDJSON) {
                ndjsonReader = Files.newBufferedReader(path, charset);
            } else {
                if (!(jsonParser instanceof DefaultJsonParser defaultParser)) {
                    throw new IllegalArgumentException(
                            "Chunked JSON array mode requires DefaultJsonParser, got "
                                    + jsonParser.getClass().getSimpleName());
                }
                arrayIterator = defaultParser.openArrayElementIterator(Files.newBufferedReader(path, charset));
            }
            readerOpen = true;
        } catch (IOException ex) {
            closeReaders();
            throw new IllegalStateException("Failed to open JSON source: " + source.getPath(), ex);
        }
    }

    private JsonReadFormat resolveFormat(Path path) {
        String configured = source.getFormat();
        if (configured != null && !configured.isBlank()) {
            String normalized = configured.trim().toLowerCase(Locale.ROOT);
            if (FORMAT_NDJSON.equals(normalized)) {
                return JsonReadFormat.NDJSON;
            }
            if (FORMAT_ARRAY.equals(normalized)) {
                return JsonReadFormat.ARRAY;
            }
            throw new IllegalArgumentException(
                    "Unsupported JSON source format [" + configured + "]; expected ndjson or array");
        }
        return detectFormatFromFile(path);
    }

    private JsonReadFormat detectFormatFromFile(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            int value;
            while ((value = input.read()) != -1) {
                if (!Character.isWhitespace(value)) {
                    return value == '[' ? JsonReadFormat.ARRAY : JsonReadFormat.NDJSON;
                }
            }
            // Empty file — treat as exhausted NDJSON with no rows.
            return JsonReadFormat.NDJSON;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to detect JSON format for: " + path, ex);
        }
    }

    private Map<String, Object> readNextRowValues() {
        if (readFormat == JsonReadFormat.NDJSON) {
            return readNextNdjsonRow();
        }
        return readNextArrayRow();
    }

    private Map<String, Object> readNextNdjsonRow() {
        if (!(jsonParser instanceof DefaultJsonParser defaultParser)) {
            throw new IllegalArgumentException(
                    "Chunked NDJSON mode requires DefaultJsonParser, got " + jsonParser.getClass().getSimpleName());
        }
        try {
            String line;
            while ((line = ndjsonReader.readLine()) != null) {
                ndjsonLineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                return defaultParser.parseNdjsonLine(line, ndjsonLineNumber);
            }
            return null;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read NDJSON line from source [" + name + "]", ex);
        }
    }

    private Map<String, Object> readNextArrayRow() {
        if (arrayIterator == null || !arrayIterator.hasNext()) {
            return null;
        }
        return arrayIterator.next();
    }

    private void inferSchemaFromRow(Map<String, Object> values) {
        if (schema != null || source.getSchema() != null) {
            if (schema == null) {
                schema = source.getSchema();
            }
            return;
        }
        RowSchema inferred = new RowSchema();
        inferred.setColumns(values.keySet().stream()
                .map(column -> new ColumnDef(column, "VARCHAR", true))
                .toList());
        schema = inferred;
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

    private void closeReaders() {
        if (ndjsonReader != null) {
            try {
                ndjsonReader.close();
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to close NDJSON reader for source [" + name + "]", ex);
            }
            ndjsonReader = null;
        }
        if (arrayIterator != null) {
            arrayIterator.close();
            arrayIterator = null;
        }
        readerOpen = false;
    }

    private enum JsonReadFormat {
        NDJSON,
        ARRAY
    }
}
