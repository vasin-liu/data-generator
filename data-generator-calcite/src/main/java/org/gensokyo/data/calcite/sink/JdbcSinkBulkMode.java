/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * JDBC sink bulk-write strategies selected via writer {@code options.bulkMode}.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
enum JdbcSinkBulkMode {
    /** Named-parameter batch insert (default). */
    JDBC_BATCH,
    /** PostgreSQL {@code COPY ... FROM STDIN} CSV stream. */
    POSTGRES_COPY,
    /** ClickHouse multi-row {@code INSERT ... VALUES} statement. */
    CLICKHOUSE_INSERT;

    /**
     * Resolves the bulk mode from writer options.
     *
     * @param writer JDBC writer configuration
     * @return resolved bulk mode
     */
    static JdbcSinkBulkMode resolve(JdbcWriterVO writer) {
        String bulkMode = WriterOptionResolver.stringOption(writer, "bulkMode", null);
        if (!StringUtils.hasText(bulkMode)) {
            return JDBC_BATCH;
        }
        return switch (bulkMode.trim().toLowerCase(Locale.ROOT)) {
            case "jdbc_batch", "batch" -> JDBC_BATCH;
            case "postgres_copy", "copy" -> POSTGRES_COPY;
            case "clickhouse_insert", "clickhouse" -> CLICKHOUSE_INSERT;
            default -> throw new IllegalArgumentException("Unsupported JDBC sink bulkMode: " + bulkMode);
        };
    }
}
