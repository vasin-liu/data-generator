package org.gensokyo.data.calcite;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JdbcRowSinkAdapter implements RowSink {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcWriterVO writer;

    public JdbcRowSinkAdapter(NamedParameterJdbcTemplate jdbcTemplate, JdbcWriterVO writer) {
        this.jdbcTemplate = jdbcTemplate;
        this.writer = writer;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String sql = buildSql(schema);
        Map<String, ?>[] batch = rows.stream()
                .map(this::toSqlParams)
                .toArray(Map[]::new);
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(writer.getDataSourceId()));
            jdbcTemplate.batchUpdate(sql, batch);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private String buildSql(RowSchema schema) {
        if (schema == null || schema.getColumns() == null || schema.getColumns().isEmpty()) {
            throw new IllegalArgumentException("JDBC sink requires at least one output column");
        }
        String table = Objects.requireNonNull(writer.getTarget(), "JDBC sink target must not be null");
        String columns = schema.getColumns().stream()
                .map(column -> column.getName())
                .toList()
                .stream()
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        String values = schema.getColumns().stream()
                .map(column -> ":" + column.getName())
                .toList()
                .stream()
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        return "insert into " + table + " (" + columns + ") values (" + values + ")";
    }

    private Map<String, Object> toSqlParams(Row row) {
        Map<String, Object> params = new LinkedHashMap<>();
        row.values().forEach(params::put);
        return params;
    }
}
