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
    private final CsvParser csvParser;

    public CsvRowSource(String name, CsvSourceVO source) {
        this(name, source, new DefaultCsvParser());
    }

    public CsvRowSource(String name, CsvSourceVO source, CsvParser csvParser) {
        this.name = name;
        this.source = source;
        this.csvParser = csvParser;
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
        List<List<String>> records = records();
        List<String> columns = source.isHeader() && !records.isEmpty()
                ? records.getFirst()
                : generatedColumns(firstDataColumnCount(records));
        RowSchema schema = new RowSchema();
        schema.setColumns(columns.stream()
                .map(column -> new ColumnDef(column, "VARCHAR", true))
                .toList());
        return schema;
    }

    @Override
    public List<Row> rows() {
        List<List<String>> records = records();
        if (records.isEmpty()) {
            return List.of();
        }
        List<String> columns = columnNames(records);
        int startIndex = source.isHeader() ? 1 : 0;
        long limit = source.getMaxRows() == null ? Long.MAX_VALUE : source.getMaxRows();
        List<Row> rows = new ArrayList<>();
        for (int i = startIndex; i < records.size() && rows.size() < limit; i++) {
            if (records.get(i).isEmpty()) {
                continue;
            }
            rows.add(toRow(columns, records.get(i), i + 1));
        }
        return rows;
    }

    private List<String> columnNames(List<List<String>> records) {
        if (source.getSchema() != null && source.getSchema().getColumns() != null
                && !source.getSchema().getColumns().isEmpty()) {
            return source.getSchema().getColumns().stream()
                    .map(ColumnDef::getName)
                    .toList();
        }
        if (source.isHeader() && !records.isEmpty()) {
            return records.getFirst();
        }
        return generatedColumns(firstDataColumnCount(records));
    }

    private Row toRow(List<String> columns, List<String> values, int lineNumber) {
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

    private List<List<String>> records() {
        return csvParser.parse(source, readLines()).stream()
                .filter(record -> !record.stream().allMatch(String::isBlank))
                .toList();
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

    private int firstDataColumnCount(List<List<String>> records) {
        int index = source.isHeader() ? 1 : 0;
        if (index >= records.size()) {
            return 0;
        }
        return records.get(index).size();
    }

    private List<String> generatedColumns(int count) {
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            columns.add("c" + (i + 1));
        }
        return columns;
    }

}
