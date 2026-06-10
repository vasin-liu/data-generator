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
    void buildsPostgresUpsertWhenDialectAndConflictColumnsAreSet() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of(
                "dialect", "postgres",
                "upsert", true,
                "conflictColumns", "id"));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
        Assertions.assertEquals(
                "insert into orders_out (id, amount) values (:id, :amount) on conflict (id) do nothing",
                sql);
    }

    @Test
    void buildsMysqlInsertIgnoreWhenDialectIsMysql() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of("dialect", "mysql", "upsert", true));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
        Assertions.assertEquals(
                "insert ignore into orders_out (id, amount) values (:id, :amount)",
                sql);
    }

    private static JdbcWriterVO writer(String target) {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setTarget(target);
        return writer;
    }
}
