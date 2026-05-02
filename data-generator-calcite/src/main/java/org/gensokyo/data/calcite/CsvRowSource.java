package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvRowSource implements RowSource {
    private final String name;
    private final CsvSourceVO source;

    public CsvRowSource(String name, CsvSourceVO source) {
        this.name = name;
        this.source = source;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RowSchema schema() {
        if (source.getSchema() != null) {
            return source.getSchema();
        }
        List<String> lines = readLines();
        List<String> columns = source.isHeader() && !lines.isEmpty()
                ? parseLine(lines.getFirst(), delimiter())
                : generatedColumns(firstDataColumnCount(lines));
        RowSchema schema = new RowSchema();
        schema.setColumns(columns.stream()
                .map(column -> new ColumnDef(column, "VARCHAR", true))
                .toList());
        return schema;
    }

    @Override
    public List<Row> rows() {
        List<String> lines = readLines();
        if (lines.isEmpty()) {
            return List.of();
        }
        List<String> columns = columnNames(lines);
        int startIndex = source.isHeader() ? 1 : 0;
        long limit = source.getMaxRows() == null ? Long.MAX_VALUE : source.getMaxRows();
        List<Row> rows = new ArrayList<>();
        for (int i = startIndex; i < lines.size() && rows.size() < limit; i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            rows.add(toRow(columns, parseLine(lines.get(i), delimiter())));
        }
        return rows;
    }

    private List<String> columnNames(List<String> lines) {
        if (source.getSchema() != null && source.getSchema().getColumns() != null
                && !source.getSchema().getColumns().isEmpty()) {
            return source.getSchema().getColumns().stream()
                    .map(ColumnDef::getName)
                    .toList();
        }
        if (source.isHeader() && !lines.isEmpty()) {
            return parseLine(lines.getFirst(), delimiter());
        }
        return generatedColumns(firstDataColumnCount(lines));
    }

    private Row toRow(List<String> columns, List<String> values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            row.put(columns.get(i), i < values.size() ? values.get(i) : null);
        }
        return new Row(row);
    }

    private List<String> readLines() {
        if (source.getPath() == null || source.getPath().isBlank()) {
            throw new IllegalArgumentException("CSV source path must not be blank");
        }
        try {
            return Files.readAllLines(Path.of(source.getPath()), Charset.forName(source.getCharset()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV source: " + source.getPath(), e);
        }
    }

    private int firstDataColumnCount(List<String> lines) {
        int index = source.isHeader() ? 1 : 0;
        if (index >= lines.size()) {
            return 0;
        }
        return parseLine(lines.get(index), delimiter()).size();
    }

    private List<String> generatedColumns(int count) {
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            columns.add("c" + (i + 1));
        }
        return columns;
    }

    private char delimiter() {
        String delimiter = source.getDelimiter();
        return delimiter == null || delimiter.isEmpty() ? ',' : delimiter.charAt(0);
    }

    private List<String> parseLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == delimiter && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}
