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
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

/**
 * Integration tests for ClickHouse multi-row INSERT bulk JDBC sink writes.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
@Testcontainers(disabledWithoutDocker = true)
class ClickHouseInsertBulkWriterIntegrationTests {

    @Container
    private static final ClickHouseContainer CLICKHOUSE = new ClickHouseContainer(
            DockerImageName.parse("clickhouse/clickhouse-server:24.8"));

    private static DriverManagerDataSource dataSource;
    private static NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Starts ClickHouse and creates the target MergeTree table.
     */
    @BeforeAll
    static void startDatabase() {
        CLICKHOUSE.start();
        dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        dataSource.setUrl(CLICKHOUSE.getJdbcUrl());
        dataSource.setUsername(CLICKHOUSE.getUsername());
        dataSource.setPassword(CLICKHOUSE.getPassword());
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS orders_out (
                    id UInt64,
                    amount Float64,
                    note Nullable(String)
                ) ENGINE = MergeTree
                ORDER BY id
                """);
    }

    /**
     * Releases static datasource references after the container stops.
     */
    @AfterAll
    static void stopDatabase() {
        dataSource = null;
    }

    /**
     * Verifies {@code bulkMode=clickhouse_insert} persists rows via multi-value INSERT.
     */
    @Test
    void writesRowsUsingClickHouseInsertBulkMode() {
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE orders_out");

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId("ignored");
        writer.setTarget("orders_out");
        writer.setTemplate("id:value, amount:amount, note:note");
        writer.setOptions(Map.of("bulkMode", "clickhouse_insert", "dialect", "clickhouse"));

        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("value", "UInt64", false),
                new ColumnDef("amount", "Float64", false),
                new ColumnDef("note", "String", true)));

        List<Row> rows = List.of(
                row(Map.of("value", 1L, "amount", 10.5d, "note", "alpha")),
                row(Map.of("value", 2L, "amount", 20.0d, "note", "beta, 'quoted'")));

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
        Assertions.assertEquals("beta, 'quoted'", persisted.get(1).get("note"));
    }

    private static Row row(Map<String, Object> values) {
        return new Row(values);
    }
}
