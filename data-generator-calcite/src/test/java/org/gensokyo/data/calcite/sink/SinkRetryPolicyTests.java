/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.RowSink;
import org.gensokyo.data.calcite.runtime.RunMetrics;
import org.gensokyo.data.calcite.runtime.SinkWriteMetric;
import org.gensokyo.data.calcite.runtime.SinkWriteExecutor;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for sink retry policy and partial-success metrics on {@link SinkWriteExecutor}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
class SinkRetryPolicyTests {

    @Test
    void jdbcSinkFailsTwiceThenSucceedsWithMaxRetriesThree() {
        DataSource dataSource = dataSource("sink_retry_success");
        NamedParameterJdbcTemplate delegate = new NamedParameterJdbcTemplate(dataSource);
        delegate.getJdbcTemplate().execute("create table sink_retry_out(col_value bigint)");

        IntermittentNamedParameterJdbcTemplate jdbcTemplate =
                new IntermittentNamedParameterJdbcTemplate(delegate, 2);

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("sink_retry_out");
        writer.setTemplate("col_value:value");

        SinkExecutionPolicyVO policy = new SinkExecutionPolicyVO();
        policy.setMaxRetries(3);
        policy.setRetryBackoffMs(0);

        JdbcRowSinkAdapter sink = new JdbcRowSinkAdapter(
                jdbcTemplate, writer, new NoopRuntimeJdbcEndpointResolver(), policy);

        RowSchema schema = schema(new ColumnDef("value", "BIGINT", false));
        List<Row> rows = List.of(new Row(Map.of("value", 42L)));

        sink.write(schema, rows);

        Integer count = delegate.getJdbcTemplate()
                .queryForObject("select count(*) from sink_retry_out", Integer.class);
        Assertions.assertEquals(1, count);
        Assertions.assertEquals(3, jdbcTemplate.getBatchUpdateCalls());
    }

    @Test
    void continueOnErrorCollectsPerSinkPartialSuccessMetrics() {
        RunMetrics metrics = new RunMetrics("IN_MEMORY");

        WriteStageVO failingSink = new WriteStageVO();
        failingSink.setWriters(List.of(new FailingWriterVO()));

        WriteStageVO okSink = new WriteStageVO();
        okSink.setWriters(List.of(new OkWriterVO()));

        SinkExecutionPolicyVO policy = new SinkExecutionPolicyVO();
        policy.setMode("CONTINUE_ON_ERROR");

        TemplateV2VO template = new TemplateV2VO();
        template.setSinkExecutionPolicy(policy);
        template.setSinks(List.of(failingSink, okSink));

        RowSchema schema = schema(new ColumnDef("value", "BIGINT", false));
        List<Row> rows = List.of(
                new Row(Map.of("value", 1L)),
                new Row(Map.of("value", 2L)));

        CalciteRowTransformer.TransformResult result =
                new CalciteRowTransformer.TransformResult(schema, rows);

        SinkWriteExecutor.writeSinks(
                (registry, writer) -> createSink(writer),
                null,
                template,
                result,
                metrics,
                0);

        Map<String, SinkWriteMetric> sinkMetrics = metrics.getSinkMetrics();
        Assertions.assertEquals(2, sinkMetrics.size());

        SinkWriteMetric failingMetric = sinkMetrics.get("sink[0].writer[0]");
        Assertions.assertNotNull(failingMetric);
        Assertions.assertEquals(0L, failingMetric.getRowsOk());
        Assertions.assertEquals(2L, failingMetric.getRowsFailed());
        Assertions.assertNotNull(failingMetric.getLastErrorSample());
        Assertions.assertTrue(failingMetric.getLastErrorSample().contains("Intentional sink failure"));
        Assertions.assertTrue(failingMetric.getLastErrorSample().length() <= 500);

        SinkWriteMetric okMetric = sinkMetrics.get("sink[1].writer[0]");
        Assertions.assertNotNull(okMetric);
        Assertions.assertEquals(2L, okMetric.getRowsOk());
        Assertions.assertEquals(0L, okMetric.getRowsFailed());
        Assertions.assertNull(okMetric.getLastErrorSample());
    }

    @Test
    void parallelSinksWritesAllConfiguredWriters() {
        RunMetrics metrics = new RunMetrics("IN_MEMORY");

        WriteStageVO sinkA = new WriteStageVO();
        sinkA.setWriters(List.of(new OkWriterVO()));

        WriteStageVO sinkB = new WriteStageVO();
        sinkB.setWriters(List.of(new OkWriterVO()));

        SinkExecutionPolicyVO policy = new SinkExecutionPolicyVO();
        policy.setParallelSinks(true);

        TemplateV2VO template = new TemplateV2VO();
        template.setSinkExecutionPolicy(policy);
        template.setSinks(List.of(sinkA, sinkB));

        RowSchema schema = schema(new ColumnDef("value", "BIGINT", false));
        List<Row> rows = List.of(new Row(Map.of("value", 1L)));
        CalciteRowTransformer.TransformResult result =
                new CalciteRowTransformer.TransformResult(schema, rows);

        SinkWriteExecutor.writeSinks(
                (registry, writer) -> createSink(writer),
                null,
                template,
                result,
                metrics,
                0);

        Assertions.assertEquals(2L, metrics.getRowsWritten());
    }

    @Test
    void lastErrorSampleIsTruncatedToFiveHundredCharacters() {
        RunMetrics metrics = new RunMetrics("IN_MEMORY");

        WriteStageVO failingSink = new WriteStageVO();
        failingSink.setWriters(List.of(new LongMessageFailingWriterVO()));

        SinkExecutionPolicyVO policy = new SinkExecutionPolicyVO();
        policy.setMode("CONTINUE_ON_ERROR");

        TemplateV2VO template = new TemplateV2VO();
        template.setSinkExecutionPolicy(policy);
        template.setSinks(List.of(failingSink));

        RowSchema schema = schema(new ColumnDef("value", "BIGINT", false));
        List<Row> rows = List.of(new Row(Map.of("value", 1L)));
        CalciteRowTransformer.TransformResult result =
                new CalciteRowTransformer.TransformResult(schema, rows);

        SinkWriteExecutor.writeSinks(
                (registry, writer) -> createSink(writer),
                null,
                template,
                result,
                metrics,
                0);

        SinkWriteMetric failingMetric = metrics.getSinkMetrics().get("sink[0].writer[0]");
        Assertions.assertNotNull(failingMetric);
        Assertions.assertEquals(500, failingMetric.getLastErrorSample().length());
    }

    private static RowSink createSink(WriterVO writer) {
        if (writer instanceof FailingWriterVO) {
            return new FailingRowSink();
        }
        if (writer instanceof LongMessageFailingWriterVO) {
            return new LongMessageFailingRowSink();
        }
        return new OkRowSink();
    }

    private static RowSchema schema(ColumnDef... columns) {
        RowSchema rowSchema = new RowSchema();
        rowSchema.setColumns(List.of(columns));
        return rowSchema;
    }

    private static DriverManagerDataSource dataSource(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    /**
     * Named parameter JDBC template that fails the first {@code failuresBeforeSuccess} batch updates.
     */
    private static final class IntermittentNamedParameterJdbcTemplate extends NamedParameterJdbcTemplate {
        private final NamedParameterJdbcTemplate delegate;
        private final int failuresBeforeSuccess;
        private final AtomicInteger batchUpdateCalls = new AtomicInteger();

        private IntermittentNamedParameterJdbcTemplate(
                NamedParameterJdbcTemplate delegate,
                int failuresBeforeSuccess) {
            super(delegate.getJdbcTemplate());
            this.delegate = delegate;
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int[] batchUpdate(String sql, Map<String, ?>[] batchArgs) {
            int attempt = batchUpdateCalls.incrementAndGet();
            if (attempt <= failuresBeforeSuccess) {
                throw new DataAccessResourceFailureException("simulated transient failure #" + attempt);
            }
            return delegate.batchUpdate(sql, batchArgs);
        }

        int getBatchUpdateCalls() {
            return batchUpdateCalls.get();
        }
    }

    private static final class FailingWriterVO extends WriterVO {
        private FailingWriterVO() {
            setType("FAILING");
            setTarget("failing_target");
        }
    }

    private static final class LongMessageFailingWriterVO extends WriterVO {
        private LongMessageFailingWriterVO() {
            setType("LONG_FAILING");
            setTarget("long_failing_target");
        }
    }

    private static final class OkWriterVO extends WriterVO {
        private OkWriterVO() {
            setType("OK");
            setTarget("ok_target");
        }
    }

    private static final class FailingRowSink implements RowSink {
        @Override
        public void write(RowSchema schema, List<Row> rows) {
            throw new IllegalStateException("Intentional sink failure");
        }
    }

    private static final class LongMessageFailingRowSink implements RowSink {
        @Override
        public void write(RowSchema schema, List<Row> rows) {
            throw new IllegalStateException("x".repeat(600));
        }
    }

    private static final class OkRowSink implements RowSink {
        @Override
        public void write(RowSchema schema, List<Row> rows) {
            // no-op success
        }
    }
}
