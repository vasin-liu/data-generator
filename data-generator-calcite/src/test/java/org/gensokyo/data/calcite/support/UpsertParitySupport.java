/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.support;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.runtime.SinkWriteMetric;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared CHUNKED JDBC upsert idempotency scenario for PostgreSQL, MySQL, Kingbase, and HighGo (D-15).
 *
 * <p><strong>PostgreSQL proxy for Kingbase/HighGo (D-15):</strong> {@code assertUpsertIdempotent}
 * sets sink {@code options.dialect} to the passed dialect key ({@code kingbase} or {@code highgo})
 * while the JDBC connection may use the PostgreSQL driver against a PG Testcontainers instance.
 * SQL generation follows the shared {@code ON CONFLICT} path from {@code JdbcSinkSqlBuilder}; the
 * proxy proves dialect-key mapping and {@code rowsUpserted} metrics without licensed KB/HG images.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
public final class UpsertParitySupport {

    public static final int ROW_COUNT = 500;
    public static final int SOURCE_CHUNK_SIZE = 200;
    public static final int SINK_BATCH_SIZE = 100;

    private UpsertParitySupport() {
    }

    /**
     * Seeds source, runs upsert template twice with changed non-key values, and asserts idempotency.
     *
     * @param jdbcUrl         JDBC URL (MySQL with {@code useCursorFetch} or PostgreSQL)
     * @param username        database user
     * @param password        database password
     * @param driverClassName JDBC driver class
     * @param dialect         sink {@code options.dialect} key ({@code postgres}, {@code mysql},
     *                        {@code kingbase}, or {@code highgo}); JDBC URL/driver may differ for PG-proxy ITs
     */
    public static void assertUpsertIdempotent(
            String jdbcUrl, String username, String password, String driverClassName, String dialect) {
        DataSource dataSource = dataSource(jdbcUrl, username, password, driverClassName);
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        jdbcTemplate.getJdbcTemplate().execute("drop table if exists upsert_target_t");
        jdbcTemplate.getJdbcTemplate().execute("drop table if exists upsert_source_t");
        jdbcTemplate.getJdbcTemplate().execute(
                "create table upsert_source_t(id bigint primary key, name varchar(64))");
        jdbcTemplate.getJdbcTemplate().execute(
                "create table upsert_target_t(id bigint primary key, name varchar(64))");

        seedSourceTable(jdbcTemplate, "n");

        TemplateV2VO template = upsertTemplate(dialect);
        TemplateV2RuntimeRegistry registry = registry(jdbcTemplate);

        TemplateV2RunResult firstRun = new TemplateV2Runner(registry).run(template);
        Assertions.assertNotNull(firstRun.getMetrics());
        long countAfterFirst = countRows(jdbcTemplate, "upsert_target_t");
        Assertions.assertEquals(ROW_COUNT, countAfterFirst, "first run should insert all rows");

        updateSourceNames(jdbcTemplate, "u");

        TemplateV2RunResult secondRun;
        try {
            secondRun = new TemplateV2Runner(registry).run(template);
        }
        catch (AssertionError | RuntimeException ex) {
            throw withDialectSqlHint(ex, dialect);
        }

        long countAfterSecond = countRows(jdbcTemplate, "upsert_target_t");
        Assertions.assertEquals(
                countAfterFirst,
                countAfterSecond,
                "second upsert run must not increase row count (D-15); dialect=" + dialect);

        String updatedName = jdbcTemplate.getJdbcTemplate()
                .queryForObject("select name from upsert_target_t where id = 0", String.class);
        Assertions.assertEquals("u0", updatedName, "non-key column should reflect second-run source values");

        SinkWriteMetric sinkMetric = secondRun.getMetrics().getSinkMetrics().get("sink[0].writer[0]");
        Assertions.assertNotNull(sinkMetric, "sink metrics missing for upsert writer");
        Assertions.assertTrue(
                sinkMetric.getRowsUpserted() > 0,
                "second run should record rowsUpserted > 0; dialect=" + dialect);
    }

    private static TemplateV2VO upsertTemplate(String dialect) {
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("select id, name from upsert_source_t order by id");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select id, name from t");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("upsert_target_t");
        writer.setOptions(new LinkedHashMap<>(Map.of(
                "dialect", dialect,
                "upsert", true,
                "upsertKeys", List.of("id"))));

        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(writer));

        ExecutionPolicyVO executionPolicy = new ExecutionPolicyVO();
        executionPolicy.setMode("CHUNKED");
        executionPolicy.setSourceChunkSize(SOURCE_CHUNK_SIZE);
        executionPolicy.setSinkBatchSize(SINK_BATCH_SIZE);
        executionPolicy.setMaxRowsInMemory(ROW_COUNT + 1);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("chunked-upsert-" + dialect);
        template.setExecutionPolicy(executionPolicy);
        template.setSources(Map.of("t", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static TemplateV2RuntimeRegistry registry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new JdbcSinkFactory(jdbcTemplate, new NoopRuntimeJdbcEndpointResolver())));
    }

    private static void seedSourceTable(NamedParameterJdbcTemplate jdbcTemplate, String prefix) {
        for (int batch = 0; batch < ROW_COUNT; batch += 100) {
            StringBuilder insert = new StringBuilder("insert into upsert_source_t(id, name) values ");
            for (int i = batch; i < Math.min(batch + 100, ROW_COUNT); i++) {
                if (i > batch) {
                    insert.append(',');
                }
                insert.append('(').append(i).append(", '").append(prefix).append(i).append("')");
            }
            jdbcTemplate.getJdbcTemplate().execute(insert.toString());
        }
    }

    private static void updateSourceNames(NamedParameterJdbcTemplate jdbcTemplate, String prefix) {
        for (int i = 0; i < ROW_COUNT; i++) {
            jdbcTemplate.getJdbcTemplate().update(
                    "update upsert_source_t set name = ? where id = ?",
                    prefix + i,
                    i);
        }
    }

    private static long countRows(NamedParameterJdbcTemplate jdbcTemplate, String table) {
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0L : count;
    }

    private static RuntimeException withDialectSqlHint(Throwable cause, String dialect) {
        // Kingbase and HighGo share the PostgreSQL ON CONFLICT upsert path (D-01, D-15).
        String fragment = usesPostgresConflictClause(dialect) ? "ON CONFLICT" : "ON DUPLICATE KEY";
        String message = "Upsert pipeline failed; expected generated SQL to contain " + fragment
                + " for dialect " + dialect + ": " + cause.getMessage();
        if (cause instanceof RuntimeException runtimeException) {
            runtimeException.addSuppressed(new AssertionError(message));
            return runtimeException;
        }
        return new IllegalStateException(message, cause);
    }

    private static boolean usesPostgresConflictClause(String dialect) {
        return switch (dialect) {
            case "postgres", "postgresql", "kingbase", "highgo" -> true;
            default -> false;
        };
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
