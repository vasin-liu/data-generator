package org.gensokyo.data.calcite;

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

public class CsvRowSinkAdapter implements RowSink {
    private final WriterVO writer;

    public CsvRowSinkAdapter(WriterVO writer) {
        this.writer = writer;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        Path path = targetPath();
        Charset charset = Charset.forName(stringOption("charset", StandardCharsets.UTF_8.name()));
        String delimiter = stringOption("delimiter", ",");
        boolean header = booleanOption("header", true);
        boolean append = booleanOption("append", false);
        List<String> columns = columns(schema, rows);
        List<String> lines = new ArrayList<>();
        if (header && (!append || !Files.exists(path))) {
            lines.add(csvLine(columns, delimiter));
        }
        for (Row row : rows) {
            lines.add(csvLine(columns.stream()
                    .map(column -> {
                        Object value = row.get(column);
                        return value == null ? "" : value.toString();
                    })
                    .toList(), delimiter));
        }
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

    private boolean booleanOption(String name, boolean defaultValue) {
        Object value = writer.getOptions() == null ? null : writer.getOptions().get(name);
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }
}
