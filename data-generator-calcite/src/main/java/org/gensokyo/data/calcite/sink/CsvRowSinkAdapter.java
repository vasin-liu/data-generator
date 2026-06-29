/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CSV file sink for Template V2 pipelines; supports one-shot and per-chunk streaming append.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class CsvRowSinkAdapter implements StreamingFileRowSink {
    private final WriterVO writer;
    private boolean streaming;
    private boolean initialized;

    /**
     * Creates a CSV sink for the given writer configuration.
     *
     * @param writer writer VO with target path and options
     */
    public CsvRowSinkAdapter(WriterVO writer) {
        this.writer = writer;
    }

    @Override
    public void enableStreaming() {
        this.streaming = true;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        if (streaming) {
            writeStreaming(schema, rows);
            return;
        }
        writeOneShot(schema, rows);
    }

    @Override
    public void finish() {
        // CSV streaming has no trailing delimiter or bracket to close.
    }

    private void writeOneShot(RowSchema schema, List<Row> rows) {
        Path path = targetPath();
        Charset charset = charset();
        String delimiter = delimiter();
        boolean header = booleanOption("header", true);
        boolean append = booleanOption("append", false);
        List<String> columns = columns(schema, rows);
        List<String> lines = new ArrayList<>();
        if (header && (!append || !Files.exists(path))) {
            lines.add(csvLine(columns, delimiter));
        }
        appendDataLines(lines, columns, delimiter, rows);
        writeLines(path, lines, charset, append);
    }

    private void writeStreaming(RowSchema schema, List<Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Path path = targetPath();
        Charset charset = charset();
        String delimiter = delimiter();
        boolean header = booleanOption("header", true);
        List<String> columns = columns(schema, rows);
        List<String> lines = new ArrayList<>();
        if (!initialized) {
            if (header) {
                lines.add(csvLine(columns, delimiter));
            }
            appendDataLines(lines, columns, delimiter, rows);
            writeLines(path, lines, charset, false);
            initialized = true;
            return;
        }
        appendDataLines(lines, columns, delimiter, rows);
        writeLines(path, lines, charset, true);
    }

    private void appendDataLines(
            List<String> lines,
            List<String> columns,
            String delimiter,
            List<Row> rows) {
        for (Row row : rows) {
            lines.add(csvLine(columns.stream()
                    .map(column -> {
                        Object value = row.get(column);
                        return value == null ? "" : value.toString();
                    })
                    .toList(), delimiter));
        }
    }

    private void writeLines(Path path, List<String> lines, Charset charset, boolean append) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (append) {
                Files.write(path, lines, charset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.write(path, lines, charset, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write CSV sink: " + path, e);
        }
    }

    private Path targetPath() {
        return Path.of(Objects.requireNonNull(writer.getTarget(), "CSV sink target must not be null"));
    }

    private Charset charset() {
        return Charset.forName(stringOption("charset", StandardCharsets.UTF_8.name()));
    }

    private List<String> columns(RowSchema schema, List<Row> rows) {
        if (schema != null && schema.getColumns() != null && !schema.getColumns().isEmpty()) {
            return schema.getColumns().stream().map(ColumnDef::getName).toList();
        }
        if (rows != null && !rows.isEmpty()) {
            return List.copyOf(rows.getFirst().values().keySet());
        }
        throw new IllegalArgumentException("CSV sink requires at least one output column");
    }

    private String csvLine(List<String> values, String delimiter) {
        return values.stream()
                .map(value -> escape(value, delimiter))
                .reduce((left, right) -> left + delimiter + right)
                .orElse("");
    }

    private String escape(String value, String delimiter) {
        String current = value == null ? "" : value;
        boolean quoted = current.contains(delimiter)
                || current.contains("\"")
                || current.contains("\r")
                || current.contains("\n");
        String escaped = current.replace("\"", "\"\"");
        return quoted ? "\"" + escaped + "\"" : escaped;
    }

    private String stringOption(String name, String defaultValue) {
        Object value = writer.getOptions() == null ? null : writer.getOptions().get(name);
        return value == null ? defaultValue : value.toString();
    }

    private String delimiter() {
        String delimiter = stringOption("delimiter", ",");
        if (delimiter.isEmpty()) {
            return ",";
        }
        if (delimiter.length() != 1) {
            throw new IllegalArgumentException("CSV sink delimiter must be exactly one character: "
                    + writer.getTarget());
        }
        return delimiter;
    }

    private boolean booleanOption(String name, boolean defaultValue) {
        Object value = writer.getOptions() == null ? null : writer.getOptions().get(name);
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }
}
