package org.gensokyo.data.calcite;

import com.alibaba.excel.EasyExcel;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExcelRowSinkAdapter implements RowSink {
    private static final String DEFAULT_SHEET = "Sheet1";

    private final WriterVO writer;

    public ExcelRowSinkAdapter(WriterVO writer) {
        this.writer = writer;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        Path path = Path.of(Objects.requireNonNull(writer.getTarget(), "Excel sink target must not be null"));
        List<String> columns = columns(schema, rows);
        List<List<String>> headers = headers(columns);
        if (headers.size() != columns.size()) {
            throw new IllegalArgumentException("Excel sink header count mismatch for target ["
                    + writer.getTarget() + "]: expected [" + columns.size() + "] but got [" + headers.size() + "]");
        }
        List<List<Object>> data = rows.stream()
                .map(row -> columns.stream().map(row::get).toList())
                .map(ArrayList::new)
                .<List<Object>>map(list -> list)
                .toList();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            EasyExcel.write(path.toFile())
                    .head(headers)
                    .sheet(sheetName())
                    .doWrite(data);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare Excel sink target: " + path, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to write Excel sink: " + path, e);
        }
    }

    private List<String> columns(RowSchema schema, List<Row> rows) {
        if (schema != null && schema.getColumns() != null && !schema.getColumns().isEmpty()) {
            return schema.getColumns().stream().map(ColumnDef::getName).toList();
        }
        if (rows != null && !rows.isEmpty()) {
            return List.copyOf(rows.getFirst().values().keySet());
        }
        Object configuredHeaders = option("headers");
        if (configuredHeaders instanceof List<?> list && !list.isEmpty()) {
            return normalizeHeaders(list).stream()
                    .map(header -> header.isEmpty() ? "" : header.getLast())
                    .toList();
        }
        throw new IllegalArgumentException("Excel sink requires at least one output column");
    }

    private List<List<String>> headers(List<String> columns) {
        Object configured = option("headers");
        if (configured instanceof List<?> list && !list.isEmpty()) {
            return normalizeHeaders(list);
        }
        return columns.stream().map(column -> List.of(column)).toList();
    }

    private List<List<String>> normalizeHeaders(List<?> headers) {
        List<List<String>> normalized = new ArrayList<>();
        for (Object header : headers) {
            if (header instanceof List<?> nested) {
                List<String> values = nested.stream()
                        .map(item -> item == null ? "" : item.toString())
                        .toList();
                normalized.add(values);
            } else {
                normalized.add(List.of(header == null ? "" : header.toString()));
            }
        }
        return normalized;
    }

    private String sheetName() {
        Object value = option("name");
        if (value == null || value.toString().isBlank()) {
            return DEFAULT_SHEET;
        }
        return value.toString();
    }

    private Object option(String name) {
        Map<String, Object> options = writer.getOptions();
        if (options == null) {
            return null;
        }
        return options.get(name);
    }
}
