/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.RowSink;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;

import java.util.List;
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
    private final JdbcSinkWriteStats writeStats;

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
        this(jdbcTemplate, writer, runtimeJdbcEndpointResolver, null, null);
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
        this(jdbcTemplate, writer, runtimeJdbcEndpointResolver, retryPolicy, null);
    }

    /**
     * Creates a JDBC sink with optional retry policy and write stats collector.
     *
     * @param jdbcTemplate JDBC template for batch updates
     * @param writer JDBC writer configuration
     * @param runtimeJdbcEndpointResolver resolves datasource id at runtime
     * @param retryPolicy retry policy for batch writes (may be null)
     * @param writeStats optional upsert counter collector (may be null)
     */
    public JdbcRowSinkAdapter(NamedParameterJdbcTemplate jdbcTemplate,
                              JdbcWriterVO writer,
                              RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver,
                              SinkExecutionPolicyVO retryPolicy,
                              JdbcSinkWriteStats writeStats) {
        this.jdbcTemplate = jdbcTemplate;
        this.writer = writer;
        this.runtimeJdbcEndpointResolver = runtimeJdbcEndpointResolver;
        this.retryPolicy = retryPolicy;
        this.writeStats = writeStats;
    }

    /**
     * Returns a copy of this adapter configured with the given retry policy.
     *
     * @param retryPolicy retry policy for batch writes (may be null)
     * @return JDBC sink adapter with retry policy applied
     */
    public JdbcRowSinkAdapter withRetryPolicy(SinkExecutionPolicyVO retryPolicy) {
        return new JdbcRowSinkAdapter(jdbcTemplate, writer, runtimeJdbcEndpointResolver, retryPolicy, writeStats);
    }

    /**
     * Returns a copy of this adapter configured with the given write stats collector.
     *
     * @param writeStats upsert counter collector (may be null)
     * @return JDBC sink adapter with write stats applied
     */
    public JdbcRowSinkAdapter withWriteStats(JdbcSinkWriteStats writeStats) {
        return new JdbcRowSinkAdapter(jdbcTemplate, writer, runtimeJdbcEndpointResolver, retryPolicy, writeStats);
    }

    /**
     * Returns cumulative upsert row counts collected during writes, or zero when no collector is configured.
     *
     * @return rows upserted in this adapter instance
     */
    public long getRowsUpserted() {
        return writeStats == null ? 0L : writeStats.getRowsUpserted();
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
        List<JdbcSinkColumnMappings.ColumnMapping> mappings = JdbcSinkColumnMappings.resolve(schema, writer);
        String dataSourceId = runtimeJdbcEndpointResolver.resolveSinkDataSourceId(writer);
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(dataSourceId));
            for (int i = 0; i < rows.size(); i += batchSize) {
                List<Row> slice = rows.subList(i, Math.min(i + batchSize, rows.size()));
                // Retry each JDBC batch independently for transient failures.
                SinkRetryExecutor.run(
                        retryPolicy,
                        () -> JdbcBulkWriteExecutor.writeSlice(jdbcTemplate, writer, mappings, slice, writeStats));
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }
}
