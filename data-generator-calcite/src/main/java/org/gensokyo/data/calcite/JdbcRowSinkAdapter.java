package org.gensokyo.data.calcite;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.springframework.util.StringUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JdbcRowSinkAdapter implements RowSink {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcWriterVO writer;
    private final RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    public JdbcRowSinkAdapter(NamedParameterJdbcTemplate jdbcTemplate, JdbcWriterVO writer) {
        this(jdbcTemplate, writer, new NoopRuntimeJdbcEndpointResolver());
    }

    public JdbcRowSinkAdapter(NamedParameterJdbcTemplate jdbcTemplate,
                              JdbcWriterVO writer,
                              RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver) {
        this.jdbcTemplate = jdbcTemplate;
        this.writer = writer;
        this.runtimeJdbcEndpointResolver = runtimeJdbcEndpointResolver;
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<ColumnMapping> mappings = resolveMappings(schema);
        String sql = buildSql(mappings);
        Map<String, ?>[] batch = rows.stream()
                .map(row -> toSqlParams(row, mappings))
                .toArray(Map[]::new);
        String dataSourceId = runtimeJdbcEndpointResolver.resolveSinkDataSourceId(writer);
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(dataSourceId));
            jdbcTemplate.batchUpdate(sql, batch);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private List<ColumnMapping> resolveMappings(RowSchema schema) {
        if (schema == null || schema.getColumns() == null || schema.getColumns().isEmpty()) {
            throw new IllegalArgumentException("JDBC sink requires at least one output column");
        }
        if (!StringUtils.hasText(writer.getTemplate())) {
            return schema.getColumns().stream()
                    .map(column -> new ColumnMapping(column.getName(), column.getName()))
                    .toList();
        }
        List<ColumnMapping> mappings = new ArrayList<>();
        for (String token : writer.getTemplate().split(Const.COMMA)) {
            String item = token == null ? null : token.trim();
            if (!StringUtils.hasText(item)) {
                continue;
            }
            int split = item.indexOf(Const.COLON);
            if (split < 0) {
                mappings.add(new ColumnMapping(item, item));
            } else {
                String target = item.substring(0, split).trim();
                String source = item.substring(split + 1).trim();
                if (!StringUtils.hasText(target) || !StringUtils.hasText(source)) {
                    throw new IllegalArgumentException("Invalid JDBC sink template item: " + item);
                }
                mappings.add(new ColumnMapping(target, source));
            }
        }
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("JDBC sink template resolved to no columns");
        }
        return mappings;
    }

    private String buildSql(List<ColumnMapping> mappings) {
        String table = Objects.requireNonNull(writer.getTarget(), "JDBC sink target must not be null");
        String columns = mappings.stream()
                .map(ColumnMapping::target)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        String values = mappings.stream()
                .map(mapping -> ":" + mapping.target())
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        return "insert into " + table + " (" + columns + ") values (" + values + ")";
    }

    private Map<String, Object> toSqlParams(Row row, List<ColumnMapping> mappings) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (ColumnMapping mapping : mappings) {
            params.put(mapping.target(), row.get(mapping.source()));
        }
        return params;
    }

    private record ColumnMapping(String target, String source) {
    }
}
