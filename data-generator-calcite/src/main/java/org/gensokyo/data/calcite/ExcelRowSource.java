package org.gensokyo.data.calcite;

import com.alibaba.excel.EasyExcel;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.ExcelSheetSourceVO;
import org.gensokyo.data.model.v2.ExcelSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExcelRowSource implements RowSource {
    private final String name;
    private final ExcelSourceVO source;
    private volatile MaterializedExcel materialized;

    public ExcelRowSource(String name, ExcelSourceVO source) {
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
        MaterializedExcel excel = materialized();
        RowSchema schema = new RowSchema();
        schema.setColumns(excel.columns().stream()
                .map(column -> new ColumnDef(column, "VARCHAR", true))
                .toList());
        return schema;
    }

    @Override
    public List<Row> rows() {
        MaterializedExcel excel = materialized();
        List<String> columns = source.getSchema() != null && source.getSchema().getColumns() != null
                && !source.getSchema().getColumns().isEmpty()
                ? source.getSchema().getColumns().stream().map(ColumnDef::getName).toList()
                : excel.columns();
        return excel.rows().stream()
                .map(values -> new Row(toRow(columns, values)))
                .toList();
    }

    private MaterializedExcel materialized() {
        MaterializedExcel current = materialized;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (materialized == null) {
                materialized = load();
            }
            return materialized;
        }
    }

    private MaterializedExcel load() {
        if (source.getPath() == null || source.getPath().isBlank()) {
            throw new IllegalArgumentException("Excel source path must not be blank");
        }
        List<ExcelSheetSourceVO> configuredSheets = source.getSheets() == null || source.getSheets().isEmpty()
                ? List.of(new ExcelSheetSourceVO("Sheet1"))
                : source.getSheets();
        List<String> inferredColumns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        long globalLimit = source.getMaxRows() == null ? Long.MAX_VALUE : source.getMaxRows();
        for (ExcelSheetSourceVO sheet : configuredSheets) {
            if (rows.size() >= globalLimit) {
                break;
            }
            List<Map<Integer, String>> rawRows = EasyExcel.read(source.getPath())
                    .sheet(sheetName(sheet))
                    .headRowNumber(0)
                    .doReadSync();
            SheetWindow window = window(rawRows, sheet);
            if (window.columns().isEmpty()) {
                continue;
            }
            if (inferredColumns.isEmpty()) {
                inferredColumns.addAll(window.columns());
            }
            for (List<String> values : window.rows()) {
                rows.add(values);
                if (rows.size() >= globalLimit) {
                    break;
                }
            }
        }
        return new MaterializedExcel(
                inferredColumns.isEmpty() ? generatedColumns(maxWidth(rows)) : inferredColumns,
                rows
        );
    }

    private SheetWindow window(List<Map<Integer, String>> rawRows, ExcelSheetSourceVO sheet) {
        List<String> columns = explicitColumns(sheet);
        List<List<String>> rows = new ArrayList<>();
        int startRow = Math.max(sheet.getStartRow(), 1);
        int endRow = sheet.getEndRow() < 1 ? Integer.MAX_VALUE : sheet.getEndRow();
        if (rawRows.isEmpty() || startRow > rawRows.size()) {
            return new SheetWindow(columns, rows);
        }

        if (columns.isEmpty()) {
            columns = rowValues(rawRows.get(startRow - 1));
        }
        int firstDataIndex = startRow;
        int lastDataIndexExclusive = Math.min(rawRows.size(), endRow);
        for (int i = firstDataIndex; i < lastDataIndexExclusive; i++) {
            List<String> values = rowValues(rawRows.get(i));
            if (values.stream().allMatch(String::isBlank)) {
                continue;
            }
            rows.add(values);
        }
        return new SheetWindow(columns, rows);
    }

    private String sheetName(ExcelSheetSourceVO sheet) {
        if (sheet == null || sheet.getName() == null || sheet.getName().isBlank()) {
            return "Sheet1";
        }
        return sheet.getName();
    }

    private List<String> explicitColumns(ExcelSheetSourceVO sheet) {
        if (sheet == null || sheet.getHeaders() == null || sheet.getHeaders().isEmpty()) {
            return List.of();
        }
        return sheet.getHeaders().stream()
                .map(header -> header == null || header.isEmpty() ? "" : Objects.toString(header.getLast(), ""))
                .toList();
    }

    private List<String> rowValues(Map<Integer, String> row) {
        if (row == null || row.isEmpty()) {
            return List.of();
        }
        int width = row.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        List<String> values = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            values.add(Objects.toString(row.get(i), ""));
        }
        return values;
    }

    private Map<String, Object> toRow(List<String> columns, List<String> values) {
        Map<String, Object> row = new LinkedHashMap<>();
        int width = Math.max(columns.size(), values.size());
        List<String> effectiveColumns = columns.size() >= width ? columns : paddedColumns(columns, width);
        for (int i = 0; i < effectiveColumns.size(); i++) {
            row.put(effectiveColumns.get(i), i < values.size() ? values.get(i) : null);
        }
        return row;
    }

    private List<String> paddedColumns(List<String> columns, int width) {
        List<String> padded = new ArrayList<>(columns);
        for (int i = columns.size(); i < width; i++) {
            padded.add("c" + (i + 1));
        }
        return padded;
    }

    private List<String> generatedColumns(int count) {
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            columns.add("c" + (i + 1));
        }
        return columns;
    }

    private int maxWidth(List<List<String>> rows) {
        return rows.stream().mapToInt(List::size).max().orElse(0);
    }

    private record MaterializedExcel(List<String> columns, List<List<String>> rows) {
    }

    private record SheetWindow(List<String> columns, List<List<String>> rows) {
    }
}
