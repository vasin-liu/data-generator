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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

/**
 * Integration tests for PostgreSQL COPY bulk JDBC sink writes.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresCopyBulkWriterIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("dg_bulk")
            .withUsername("dg")
            .withPassword("dg");

    private static DriverManagerDataSource dataSource;
    private static NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeAll
    static void startDatabase() {
        POSTGRES.start();
        dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE orders_out (
                    id BIGINT PRIMARY KEY,
                    amount NUMERIC(12, 2),
                    note TEXT
                )
                """);
    }

    @AfterAll
    static void stopDatabase() {
        dataSource = null;
    }

    @Test
    void writesRowsUsingPostgresCopyBulkMode() {
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE orders_out");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("orders_out");
        writer.setTemplate("id:value, amount:amount, note:note");
        writer.setOptions(Map.of("bulkMode", "postgres_copy", "dialect", "postgres"));

        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("value", "BIGINT", false),
                new ColumnDef("amount", "NUMERIC", false),
                new ColumnDef("note", "TEXT", true)));

        List<Row> rows = List.of(
                row(Map.of("value", 1L, "amount", 10.5d, "note", "alpha")),
                row(Map.of("value", 2L, "amount", 20.0d, "note", "beta, \"quoted\"")));

        JdbcRowSinkAdapter sink = new JdbcRowSinkAdapter(
                jdbcTemplate,
                writer,
                new NoopRuntimeJdbcEndpointResolver());
        sink.writeBatch(schema, rows, 2);

        List<Map<String, Object>> persisted = jdbcTemplate.getJdbcTemplate()
                .queryForList("SELECT id, amount, note FROM orders_out ORDER BY id");
        Assertions.assertEquals(2, persisted.size());
        Assertions.assertEquals(1L, ((Number) persisted.get(0).get("id")).longValue());
        Assertions.assertEquals("alpha", persisted.get(0).get("note"));
        Assertions.assertEquals("beta, \"quoted\"", persisted.get(1).get("note"));
    }

    private static Row row(Map<String, Object> values) {
        return new Row(values);
    }
}
