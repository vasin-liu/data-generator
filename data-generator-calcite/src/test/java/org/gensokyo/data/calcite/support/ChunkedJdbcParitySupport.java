/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.support;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Shared CHUNKED pipeline JDBC export scenario for dialect parity tests.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class ChunkedJdbcParitySupport {

    public static final int ROW_COUNT = 3_500;
    public static final int SOURCE_CHUNK_SIZE = 1_000;
    public static final int SINK_BATCH_SIZE = 400;

    private ChunkedJdbcParitySupport() {
    }

    /**
     * Seeds source/target tables, runs CHUNKED export, and asserts row counts and metrics.
     *
     * @param jdbcUrl       JDBC URL (MySQL with {@code useCursorFetch} or PostgreSQL)
     * @param username      database user
     * @param password      database password
     * @param driverClassName JDBC driver class
     */
    public static void assertChunkedExportParity(
            String jdbcUrl, String username, String password, String driverClassName) {
        DataSource dataSource = dataSource(jdbcUrl, username, password, driverClassName);
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        jdbcTemplate.getJdbcTemplate().execute("drop table if exists chunked_target_t");
        jdbcTemplate.getJdbcTemplate().execute("drop table if exists chunked_source_t");
        jdbcTemplate.getJdbcTemplate().execute(
                "create table chunked_source_t(id bigint primary key, name varchar(32))");
        jdbcTemplate.getJdbcTemplate().execute(
                "create table chunked_target_t(id bigint primary key, name varchar(32))");

        seedSourceTable(jdbcTemplate);

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select id, name from chunked_source_t order by id");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select id, name from t");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("chunked_target_t");

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("CHUNKED");
        executionPolicy.setSourceChunkSize(SOURCE_CHUNK_SIZE);
        executionPolicy.setSinkBatchSize(SINK_BATCH_SIZE);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("chunked-jdbc-parity");
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("t", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));

        TemplateV2RuntimeRegistry registry = new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver())));

        TemplateV2RunResult result = new TemplateV2Runner(registry).run(template);

        Assertions.assertTrue(result.getRows().isEmpty(), "CHUNKED must not retain all rows in memory");
        Assertions.assertNotNull(result.getMetrics());
        Assertions.assertEquals("CHUNKED", result.getMetrics().getExecutionMode());
        Assertions.assertEquals(ROW_COUNT, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(ROW_COUNT, countRows(jdbcTemplate, "chunked_target_t"));
        int expectedChunks = (ROW_COUNT + SOURCE_CHUNK_SIZE - 1) / SOURCE_CHUNK_SIZE;
        Assertions.assertEquals(expectedChunks, result.getMetrics().getChunksProcessed());
    }

    private static void seedSourceTable(NamedParameterJdbcTemplate jdbcTemplate) {
        for (int batch = 0; batch < ROW_COUNT; batch += 500) {
            StringBuilder insert = new StringBuilder("insert into chunked_source_t(id, name) values ");
            for (int i = batch; i < Math.min(batch + 500, ROW_COUNT); i++) {
                if (i > batch) {
                    insert.append(',');
                }
                insert.append('(').append(i).append(", 'n").append(i).append("')");
            }
            jdbcTemplate.getJdbcTemplate().execute(insert.toString());
        }
    }

    private static long countRows(NamedParameterJdbcTemplate jdbcTemplate, String table) {
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0L : count;
    }

    private static DataSource dataSource(
            String jdbcUrl, String username, String password, String driverClassName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
