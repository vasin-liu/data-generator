/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.RowSink;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JDBC row sink that batch-inserts transformed rows into a configured table.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class JdbcRowSinkAdapter implements RowSink {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcWriterVO writer;
    private final RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;
    private final SinkExecutionPolicyVO retryPolicy;

    /**
     * Creates a JDBC sink without retry policy.
     *
     * @param jdbcTemplate JDBC template for batch updates
     * @param writer JDBC writer configuration
     */
    public JdbcRowSinkAdapter(NamedParameterJdbcTemplate jdbcTemplate, JdbcWriterVO writer) {
        this(jdbcTemplate, writer, new NoopRuntimeJdbcEndpointResolver());
    }

    /**
     * Creates a JDBC sink without retry policy.
     *
     * @param jdbcTemplate JDBC template for batch updates
     * @param writer JDBC writer configuration
     * @param runtimeJdbcEndpointResolver resolves datasource id at runtime
     */
    public JdbcRowSinkAdapter(NamedParameterJdbcTemplate jdbcTemplate,
                              JdbcWriterVO writer,
                              RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver) {
        this(jdbcTemplate, writer, runtimeJdbcEndpointResolver, null);
    }

    /**
     * Creates a JDBC sink with optional retry policy.
     *
     * @param jdbcTemplate JDBC template for batch updates
     * @param writer JDBC writer configuration
     * @param runtimeJdbcEndpointResolver resolves datasource id at runtime
     * @param retryPolicy retry policy for batch writes (may be null)
     */
    public JdbcRowSinkAdapter(NamedParameterJdbcTemplate jdbcTemplate,
                              JdbcWriterVO writer,
                              RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver,
                              SinkExecutionPolicyVO retryPolicy) {
        this.jdbcTemplate = jdbcTemplate;
        this.writer = writer;
        this.runtimeJdbcEndpointResolver = runtimeJdbcEndpointResolver;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Returns a copy of this adapter configured with the given retry policy.
     *
     * @param retryPolicy retry policy for batch writes (may be null)
     * @return JDBC sink adapter with retry policy applied
     */
    public JdbcRowSinkAdapter withRetryPolicy(SinkExecutionPolicyVO retryPolicy) {
        return new JdbcRowSinkAdapter(jdbcTemplate, writer, runtimeJdbcEndpointResolver, retryPolicy);
    }

    @Override
    public void write(RowSchema schema, List<Row> rows) {
        writeBatch(schema, rows, rows == null || rows.isEmpty() ? 1 : rows.size());
    }

    @Override
    public void writeBatch(RowSchema schema, List<Row> rows, int batchSize) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        List<ColumnMapping> mappings = resolveMappings(schema);
        String sql = buildSql(mappings);
        String dataSourceId = runtimeJdbcEndpointResolver.resolveSinkDataSourceId(writer);
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(dataSourceId));
            for (int i = 0; i < rows.size(); i += batchSize) {
                List<Row> slice = rows.subList(i, Math.min(i + batchSize, rows.size()));
                Map<String, ?>[] batch = slice.stream()
                        .map(row -> toSqlParams(row, mappings))
                        .toArray(Map[]::new);
                // Retry each JDBC batch independently for transient failures.
                SinkRetryExecutor.run(retryPolicy, () -> jdbcTemplate.batchUpdate(sql, batch));
            }
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
