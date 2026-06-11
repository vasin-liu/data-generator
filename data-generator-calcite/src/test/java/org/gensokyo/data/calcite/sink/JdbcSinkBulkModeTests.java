/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Unit tests for {@link JdbcSinkBulkMode}.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
class JdbcSinkBulkModeTests {

    @Test
    void defaultsToJdbcBatchWhenBulkModeMissing() {
        JdbcWriterVO writer = new JdbcWriterVO();
        Assertions.assertEquals(JdbcSinkBulkMode.JDBC_BATCH, JdbcSinkBulkMode.resolve(writer));
    }

    @Test
    void resolvesPostgresCopyAliases() {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setOptions(Map.of("bulkMode", "copy"));
        Assertions.assertEquals(JdbcSinkBulkMode.POSTGRES_COPY, JdbcSinkBulkMode.resolve(writer));
    }

    @Test
    void resolvesClickHouseInsertAlias() {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setOptions(Map.of("bulkMode", "clickhouse"));
        Assertions.assertEquals(JdbcSinkBulkMode.CLICKHOUSE_INSERT, JdbcSinkBulkMode.resolve(writer));
    }

    @Test
    void rejectsUnknownBulkMode() {
        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setOptions(Map.of("bulkMode", "oracle_direct_path"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JdbcSinkBulkMode.resolve(writer));
    }
}
