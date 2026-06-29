/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.NoopRuntimeJdbcEndpointResolver;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * H2 smoke tests for JDBC upsert key validation and MySQL-mode upsert execution (D-25 basic path).
 * <p>
 * Full PostgreSQL/MySQL dialect proof is deferred to Testcontainers in plan 08-09.
 * H2 does not implement PostgreSQL {@code ON CONFLICT}; tests use {@code MODE=MySQL} for
 * {@code ON DUPLICATE KEY UPDATE} compatibility.
 * </p>
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
class JdbcUpsertSmokeTests {

    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = dataSource("upsert_smoke_" + System.nanoTime());
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcTemplate.getJdbcTemplate().execute("SET MODE MySQL");
        jdbcTemplate.getJdbcTemplate().execute(
                "CREATE TABLE upsert_smoke (id BIGINT PRIMARY KEY, amount BIGINT)");
    }

    @Test
    void h2SmokeValidUpsertKeysExecutesWithoutBuilderException() {
        JdbcWriterVO writer = upsertWriter(List.of("id"));
        JdbcRowSinkAdapter sink = new JdbcRowSinkAdapter(
                jdbcTemplate, writer, new NoopRuntimeJdbcEndpointResolver());

        RowSchema schema = schema();
        sink.write(schema, List.of(new Row(Map.of("id", 1L, "amount", 10L))));

        Long amount = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT amount FROM upsert_smoke WHERE id = 1", Long.class);
        Assertions.assertEquals(10L, amount);
    }

    @Test
    void invalidUpsertKeyFailsBeforeExecute() {
        JdbcWriterVO writer = upsertWriter(List.of("unknown_col"));
        JdbcRowSinkAdapter sink = new JdbcRowSinkAdapter(
                jdbcTemplate, writer, new NoopRuntimeJdbcEndpointResolver());

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> sink.write(schema(), List.of(new Row(Map.of("id", 1L, "amount", 10L)))));
        Assertions.assertTrue(ex.getMessage().contains("unknown_col"));

        Integer count = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT COUNT(*) FROM upsert_smoke", Integer.class);
        Assertions.assertEquals(0, count);
    }

    @Test
    void reRunIncrementsRowsUpserted() {
        JdbcWriterVO writer = upsertWriter(List.of("id"));
        JdbcSinkWriteStats writeStats = new JdbcSinkWriteStats();
        JdbcRowSinkAdapter sink = new JdbcRowSinkAdapter(
                jdbcTemplate, writer, new NoopRuntimeJdbcEndpointResolver(), null, writeStats);

        RowSchema schema = schema();
        sink.write(schema, List.of(new Row(Map.of("id", 1L, "amount", 10L))));
        Assertions.assertEquals(0L, writeStats.getRowsUpserted(), "first insert should not count as upsert on MySQL");

        sink.write(schema, List.of(new Row(Map.of("id", 1L, "amount", 99L))));
        Assertions.assertTrue(writeStats.getRowsUpserted() > 0, "second run should count duplicate-key updates");

        Long amount = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT amount FROM upsert_smoke WHERE id = 1", Long.class);
        Assertions.assertEquals(99L, amount);
        Integer count = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT COUNT(*) FROM upsert_smoke", Integer.class);
        Assertions.assertEquals(1, count);
    }

    @Test
    void postgresCopyRejectsUpsert() {
        JdbcWriterVO writer = upsertWriter(List.of("id"));
        writer.getOptions().put("bulkMode", "postgres_copy");
        writer.getOptions().put("dialect", "postgres");

        JdbcRowSinkAdapter sink = new JdbcRowSinkAdapter(
                jdbcTemplate, writer, new NoopRuntimeJdbcEndpointResolver());

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> sink.write(schema(), List.of(new Row(Map.of("id", 1L, "amount", 10L)))));
        Assertions.assertEquals(
                "JDBC sink bulkMode postgres_copy does not support upsert=true",
                ex.getMessage());
    }

    @Test
    void mysqlUpsertCountHeuristicTreatsDuplicateUpdateAsUpsert() {
        int[] counts = {1, 2, 2, 0};
        Assertions.assertEquals(2L, JdbcBulkWriteExecutor.countUpsertedRows(counts, "mysql"));
    }

    private static JdbcWriterVO upsertWriter(List<String> upsertKeys) {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setTarget("upsert_smoke");
        writer.setDataSourceId("primary");
        writer.setOptions(new java.util.LinkedHashMap<>(Map.of(
                "dialect", "mysql",
                "upsert", true,
                "upsertKeys", upsertKeys)));
        return writer;
    }

    private static RowSchema schema() {
        RowSchema rowSchema = new RowSchema();
        rowSchema.setColumns(List.of(
                new ColumnDef("id", "BIGINT", false),
                new ColumnDef("amount", "BIGINT", false)));
        return rowSchema;
    }

    private static DataSource dataSource(String database) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
