/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link JdbcSinkSqlBuilder}.
 *
 * @author Gensokyo
 * @since 2026-06-10
 */
class JdbcSinkSqlBuilderTests {

    @Test
    void buildsGenericInsertByDefault() {
        JdbcWriterVO writer = writer("orders_out");
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
        Assertions.assertEquals(
                "insert into orders_out (id, amount) values (:id, :amount)",
                sql);
    }

    @Test
    void buildsPostgresCompositeUpsertKeys() {
        JdbcWriterVO writer = writer("tenant_orders");
        writer.setOptions(Map.of(
                "dialect", "postgres",
                "upsert", true,
                "upsertKeys", List.of("id", "tenant_id")));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "tenant_id", "amount"));
        Assertions.assertTrue(sql.contains("on conflict (id, tenant_id) do update set"));
        Assertions.assertTrue(sql.contains("amount = excluded.amount"));
        Assertions.assertFalse(sql.contains("tenant_id = excluded.tenant_id"));
    }

    @Test
    void buildsMysqlCompositeUpsertKeys() {
        JdbcWriterVO writer = writer("tenant_orders");
        writer.setOptions(Map.of(
                "dialect", "mysql",
                "upsert", true,
                "upsertKeys", List.of("id", "tenant_id")));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "tenant_id", "amount", "status"));
        Assertions.assertTrue(sql.contains("on duplicate key update"));
        Assertions.assertTrue(sql.contains("amount = values(amount)"));
        Assertions.assertTrue(sql.contains("status = values(status)"));
        Assertions.assertFalse(sql.contains("tenant_id = values(tenant_id)"));
    }

    @Test
    void buildsPostgresUpsertWithUpsertKeysAndUpdateClause() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of(
                "dialect", "postgres",
                "upsert", true,
                "upsertKeys", List.of("id")));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
        Assertions.assertTrue(sql.contains("on conflict (id) do update set"));
        Assertions.assertTrue(sql.contains("amount = excluded.amount"));
    }

    @Test
    void buildsPostgresUpsertWhenDialectAndConflictColumnsAreSet() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of(
                "dialect", "postgres",
                "upsert", true,
                "conflictColumns", "id"));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
        Assertions.assertTrue(sql.contains("on conflict (id) do update set"));
        Assertions.assertTrue(sql.contains("amount = excluded.amount"));
    }

    @Test
    void buildsMysqlUpsertWithUpsertKeys() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of(
                "dialect", "mysql",
                "upsert", true,
                "upsertKeys", List.of("id")));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
        Assertions.assertTrue(sql.contains("on duplicate key update"));
        Assertions.assertTrue(sql.contains("amount = values(amount)"));
    }

    @Test
    void rejectsEmptyUpsertKeysWhenUpsertTrue() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of("dialect", "postgres", "upsert", true));
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount")));
        Assertions.assertTrue(ex.getMessage().contains("upsertKeys"));
        Assertions.assertTrue(ex.getMessage().contains("upsert"));
    }

    @Test
    void clickhouseUpsertIsUnsupported() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of(
                "dialect", "clickhouse",
                "upsert", true,
                "upsertKeys", List.of("id")));
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount")));
        Assertions.assertTrue(ex.getMessage().contains("clickhouse"));
    }

    @Test
    void rejectsUnknownUpsertKeyColumn() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of(
                "dialect", "mysql",
                "upsert", true,
                "upsertKeys", List.of("unknown_col")));
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount")));
        Assertions.assertTrue(ex.getMessage().contains("unknown_col"));
    }

    private static JdbcWriterVO writer(String target) {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setTarget(target);
        return writer;
    }
}
