/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.RowSink;
import org.gensokyo.data.calcite.sink.JdbcRowSinkAdapter;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for per-sink rowsRead and rowsSkipped wiring on {@link SinkWriteExecutor} (RW-04, D-16, W-03).
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
class SinkWriteExecutorMetricsTests {

    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = dataSource("sink_metrics_" + System.nanoTime());
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcTemplate.getJdbcTemplate().execute("SET MODE MySQL");
        jdbcTemplate.getJdbcTemplate().execute(
                "CREATE TABLE upsert_metrics (id BIGINT PRIMARY KEY, amount BIGINT)");
    }

    /**
     * rowsRead increments for each chunk write batch passed to a sink writer.
     */
    @Test
    void recordsRowsReadPerSinkWriteBatch() {
        RunMetrics metrics = new RunMetrics("CHUNKED");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new OkWriterVO()));

        TemplateV2VO template = new TemplateV2VO();
        template.setSinks(List.of(sink));

        RowSchema schema = schema(new ColumnDef("value", "BIGINT", false));
        List<Row> chunkOne = List.of(new Row(Map.of("value", 1L)), new Row(Map.of("value", 2L)));
        List<Row> chunkTwo = List.of(new Row(Map.of("value", 3L)));

        CalciteRowTransformer.TransformResult resultOne =
                new CalciteRowTransformer.TransformResult(schema, chunkOne);
        CalciteRowTransformer.TransformResult resultTwo =
                new CalciteRowTransformer.TransformResult(schema, chunkTwo);

        SinkWriteExecutor.writeSinks(
                (registry, writer) -> new OkRowSink(),
                null,
                template,
                resultOne,
                metrics,
                0);
        SinkWriteExecutor.writeSinks(
                (registry, writer) -> new OkRowSink(),
                null,
                template,
                resultTwo,
                metrics,
                0);

        SinkWriteMetric sinkMetric = metrics.getSinkMetrics().get("sink[0].writer[0]");
        assertThat(sinkMetric).isNotNull();
        assertThat(sinkMetric.getRowsRead()).isEqualTo(3L);
    }

    /**
     * rowsSkipped is recorded separately from rowsFailed when JDBC upsert filters null keys.
     */
    @Test
    void recordsRowsSkippedSeparatelyFromRowsFailed() {
        RunMetrics metrics = new RunMetrics("IN_MEMORY");

        JdbcWriterVO writer = upsertWriter(List.of("id"));
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        SinkExecutionPolicyVO policy = new SinkExecutionPolicyVO();
        policy.setMode("CONTINUE_ON_ERROR");

        TemplateV2VO template = new TemplateV2VO();
        template.setSinkExecutionPolicy(policy);
        template.setSinks(List.of(sink));

        RowSchema schema = schema(
                new ColumnDef("id", "BIGINT", false),
                new ColumnDef("amount", "BIGINT", false));
        List<Row> rows = List.of(
                new Row(Map.of("id", 1L, "amount", 10L)),
                new Row(new LinkedHashMap<>(Map.of("amount", 20L))));
        CalciteRowTransformer.TransformResult result =
                new CalciteRowTransformer.TransformResult(schema, rows);

        SinkWriteExecutor.writeSinks(
                (registry, w) -> new JdbcRowSinkAdapter(
                        jdbcTemplate, (JdbcWriterVO) w, new NoopRuntimeJdbcEndpointResolver()),
                null,
                template,
                result,
                metrics,
                0);

        SinkWriteMetric sinkMetric = metrics.getSinkMetrics().get("sink[0].writer[0]");
        assertThat(sinkMetric).isNotNull();
        assertThat(sinkMetric.getRowsSkipped()).isEqualTo(1L);
        assertThat(sinkMetric.getRowsRead()).isEqualTo(2L);
        assertThat(sinkMetric.getRowsFailed()).isZero();
        // CONTINUE_ON_ERROR records the full batch as ok when the JDBC write succeeds.
        assertThat(sinkMetric.getRowsOk()).isEqualTo(2L);

        Integer count = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT COUNT(*) FROM upsert_metrics", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    private static RowSchema schema(ColumnDef... columns) {
        RowSchema rowSchema = new RowSchema();
        rowSchema.setColumns(List.of(columns));
        return rowSchema;
    }

    private static JdbcWriterVO upsertWriter(List<String> upsertKeys) {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setType("jdbc");
        writer.setTarget("upsert_metrics");
        writer.setDataSourceId("primary");
        writer.setOptions(new LinkedHashMap<>(Map.of(
                "dialect", "mysql",
                "upsert", true,
                "upsertKeys", upsertKeys)));
        return writer;
    }

    private static DataSource dataSource(String database) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static final class OkWriterVO extends WriterVO {
        private OkWriterVO() {
            setType("OK");
            setTarget("ok_target");
        }
    }

    private static final class OkRowSink implements RowSink {
        @Override
        public void write(RowSchema schema, List<Row> rows) {
            // no-op success
        }
    }
}
