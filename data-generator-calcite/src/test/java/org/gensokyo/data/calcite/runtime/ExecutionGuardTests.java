/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.sink.JdbcSinkFactory;
import org.gensokyo.data.calcite.source.IteratorSourceFactory;
import org.gensokyo.data.calcite.source.QuerySourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.iterator.ConstantIteratorVO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@code maxTotalRows} execution guards across Template V2 pipelines.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
class ExecutionGuardTests {

    private static final int ROW_COUNT = 500;

    @Test
    void inMemoryModeThrowsWhenMaxTotalRowsExceeded() {
        TemplateV2VO template = iteratorTemplate("in-memory-guard", inMemoryPolicy());
        template.getExecutionPolicy().setMaxTotalRows(3);

        ExecutionLimitExceededException exception = Assertions.assertThrows(
                ExecutionLimitExceededException.class,
                () -> new TemplateV2Runner(defaultRegistry()).run(template));
        Assertions.assertTrue(exception.getMessage().contains("in-memory-guard"));
        Assertions.assertTrue(exception.getMessage().contains("maxTotalRows=3"));
    }

    @Test
    void chunkedModeThrowsWhenMaxTotalRowsExceeded() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("chunked_guard"));
        seedSourceTable(jdbcTemplate);

        TemplateV2VO template = jdbcTemplate("chunked-guard", chunkedPolicy(), jdbcTemplate);
        template.getExecutionPolicy().setMaxTotalRows(100);
        template.getExecutionPolicy().setSourceChunkSize(100);

        ExecutionLimitExceededException exception = Assertions.assertThrows(
                ExecutionLimitExceededException.class,
                () -> new TemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template));
        Assertions.assertTrue(exception.getMessage().contains("chunked-guard"));
        Assertions.assertTrue(exception.getMessage().contains("maxTotalRows=100"));
    }

    @Test
    void streamingModeThrowsWhenMaxTotalRowsExceeded() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource("streaming_guard"));
        seedSourceTable(jdbcTemplate);

        TemplateV2VO template = jdbcTemplate("streaming-guard", streamingPolicy(), jdbcTemplate);
        template.getExecutionPolicy().setMaxTotalRows(100);
        template.getExecutionPolicy().setSourceChunkSize(100);

        ExecutionLimitExceededException exception = Assertions.assertThrows(
                ExecutionLimitExceededException.class,
                () -> new TemplateV2Runner(jdbcRegistry(jdbcTemplate)).run(template));
        Assertions.assertTrue(exception.getMessage().contains("streaming-guard"));
        Assertions.assertTrue(exception.getMessage().contains("maxTotalRows=100"));
    }

    @Test
    void doesNotThrowWhenFailOnLimitExceededIsFalse() {
        TemplateV2VO template = iteratorTemplate("in-memory-no-fail", inMemoryPolicy());
        ExecutionPolicyVO policy = template.getExecutionPolicy();
        policy.setMaxTotalRows(3);
        policy.setFailOnLimitExceeded(false);

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(5, result.getRows().size());
        Assertions.assertEquals(5, result.getMetrics().getTotalRowsRead());
    }

    @Test
    void doesNotEnforceWhenMaxTotalRowsUnset() {
        TemplateV2VO template = iteratorTemplate("in-memory-unbounded", inMemoryPolicy());

        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(template);

        Assertions.assertEquals(5, result.getRows().size());
    }

    @Test
    void effectivePolicyExposesMaxTotalRowsWhenConfigured() {
        ExecutionPolicyVO vo = new ExecutionPolicyVO();
        vo.setMaxTotalRows(42);

        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(vo);

        Assertions.assertEquals(42, policy.maxTotalRows());
        Assertions.assertTrue(policy.failOnLimitExceeded());
    }

    private static TemplateV2VO iteratorTemplate(String name, ExecutionPolicyVO executionPolicy) {
        ConstantIteratorVO iterator = new ConstantIteratorVO();
        iterator.setDataset(List.of("one", "two", "three", "four", "five"));
        iterator.setRepeat(1);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);

        TemplateV2VO template = new TemplateV2VO();
        template.setName(name);
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("seed", source));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(consoleSink()));
        return template;
    }

    private static TemplateV2VO jdbcTemplate(
            String name,
            ExecutionPolicyVO executionPolicy,
            NamedParameterJdbcTemplate jdbcTemplate) {
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select id, name from source_t order by id");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("target_t");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        TemplateV2VO template = new TemplateV2VO();
        template.setName(name);
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("t", source));
        template.setTransformers(List.of(sql("select id, name from t")));
        template.setSinks(List.of(sink));
        return template;
    }

    private static ExecutionPolicyVO inMemoryPolicy() {
        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("IN_MEMORY");
        executionPolicy.setMaxRowsInMemory(1_000_000);
        executionPolicy.setFailOnLimitExceeded(true);
        return executionPolicy;
    }

    private static ExecutionPolicyVO chunkedPolicy() {
        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("CHUNKED");
        executionPolicy.setSourceChunkSize(100);
        executionPolicy.setSinkBatchSize(100);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);
        executionPolicy.setFailOnLimitExceeded(true);
        return executionPolicy;
    }

    private static ExecutionPolicyVO streamingPolicy() {
        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("STREAMING");
        executionPolicy.setSourceChunkSize(100);
        executionPolicy.setSinkBatchSize(100);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);
        executionPolicy.setFailOnLimitExceeded(true);
        return executionPolicy;
    }

    private static SqlTransformVO sql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

    private static WriteStageVO consoleSink() {
        WriteStageVO sink = new WriteStageVO();
        WriterVO writer = new ConsoleWriterVO();
        sink.setWriters(List.of(writer));
        return sink;
    }

    private static TemplateV2RuntimeRegistry defaultRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }

    private static TemplateV2RuntimeRegistry jdbcRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver())));
    }

    private static void seedSourceTable(NamedParameterJdbcTemplate jdbcTemplate) {
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
    }

    private static DataSource dataSource(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:calcite_execution_guard_" + name + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
