/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.sink.JdbcSinkFactory;
import org.gensokyo.data.calcite.source.QuerySourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for {@link StreamingPipeline} and {@link TemplateV2Runner} streaming mode.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
class StreamingPipelineTests {

    private static final int ROW_COUNT = 500;

    @Test
    void streamsQuerySourceToJdbcSinkInBatches() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table source_t(id bigint, name varchar(20))");
        jdbcTemplate.getJdbcTemplate().execute("create table target_t(id bigint, name varchar(20))");
        StringBuilder insert = new StringBuilder("insert into source_t(id, name) values ");
        for (int i = 0; i < ROW_COUNT; i++) {
            if (i > 0) {
                insert.append(',');
            }
            insert.append('(').append(i).append(", 'n").append(i).append("')");
        }
        jdbcTemplate.getJdbcTemplate().execute(insert.toString());

        TemplateV2VO template = streamingJdbcTemplate(jdbcTemplate);
        TemplateV2RunResult result = new TemplateV2Runner(streamingRegistry(jdbcTemplate)).run(template);

        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertNotNull(result.getMetrics());
        Assertions.assertEquals("STREAMING", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getRowsWritten());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertTrue(result.getMetrics().getPeakRowsInMemory() <= 100);
        Assertions.assertEquals(ROW_COUNT, countRows(jdbcTemplate, "target_t"));
        Assertions.assertEquals(ROW_COUNT / 100, result.getMetrics().getChunksProcessed());
    }

    @Test
    void rejectsMultipleSources() {
        QuerySourceVO source = querySource("select 1 as id");
        SqlTransformVO transform = passthroughTransform();

        ExecutionPolicyVO executionPolicy = streamingPolicy();
        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-multi-source");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("left", source, "right", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(jdbcSink()));

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StreamingPipeline().run(template, EffectiveExecutionPolicy.resolve(executionPolicy),
                        streamingRegistry(new NamedParameterJdbcTemplate(dataSource()))));
        Assertions.assertTrue(exception.getMessage().contains("exactly one source"));
    }

    @Test
    void rejectsBroadcastJoinShape() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource());
        jdbcTemplate.getJdbcTemplate().execute("create table dim_t(id bigint)");
        jdbcTemplate.getJdbcTemplate().execute("create table fact_t(id bigint)");
        jdbcTemplate.getJdbcTemplate().execute("insert into dim_t(id) values (1)");
        jdbcTemplate.getJdbcTemplate().execute("insert into fact_t(id) values (1)");

        QuerySourceVO dim = querySource("select id from dim_t");
        dim.setMaxRows(10L);
        QuerySourceVO fact = querySource("select id from fact_t");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("""
                select f.id
                from fact f
                left join dim d on f.id = d.id
                """);

        ExecutionPolicyVO executionPolicy = streamingPolicy();
        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-broadcast-join");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("dim", dim, "fact", fact));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(jdbcSink()));

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TemplateV2Runner(streamingRegistry(jdbcTemplate)).run(template));
        Assertions.assertTrue(exception.getMessage().contains("exactly one source")
                || exception.getMessage().contains("BROADCAST_JOIN"));
    }

    private static TemplateV2VO streamingJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
        QuerySourceVO source = querySource("select id, name from source_t order by id");

        SqlTransformVO transform = passthroughTransform();

        ExecutionPolicyVO executionPolicy = streamingPolicy();

        TemplateV2VO template = new TemplateV2VO();
        template.setName("streaming-pipeline-demo");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("t", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(jdbcSink()));
        return template;
    }

    private static ExecutionPolicyVO streamingPolicy() {
        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("STREAMING");
        executionPolicy.setSourceChunkSize(100);
        executionPolicy.setSinkBatchSize(100);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);
        return executionPolicy;
    }

    private static QuerySourceVO querySource(String sql) {
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql(sql);
        return source;
    }

    private static SqlTransformVO passthroughTransform() {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select id, name from t");
        return transform;
    }

    private static WriteStageVO jdbcSink() {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("target_t");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));
        return sink;
    }

    private static TemplateV2RuntimeRegistry streamingRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver())));
    }

    private static long countRows(NamedParameterJdbcTemplate jdbcTemplate, String table) {
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0L : count;
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:calcite_streaming_pipeline;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
