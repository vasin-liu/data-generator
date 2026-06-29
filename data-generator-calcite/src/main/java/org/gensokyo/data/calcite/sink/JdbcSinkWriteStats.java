/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

/**
 * Mutable JDBC sink batch counters collected during a single sink write job.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
public final class JdbcSinkWriteStats {

    private long rowsUpserted;
    private long rowsSkipped;

    /**
     * Rows counted as upsert updates during this sink write job.
     *
     * @return cumulative upsert row count
     */
    public long getRowsUpserted() {
        return rowsUpserted;
    }

    /**
     * Rows intentionally skipped before JDBC execute (null upsert key, etc.).
     *
     * @return cumulative skipped row count
     */
    public long getRowsSkipped() {
        return rowsSkipped;
    }

    /**
     * Adds upsert row counts from one batch slice.
     *
     * @param count rows upserted in the slice
     */
    void addRowsUpserted(long count) {
        rowsUpserted += count;
    }

    /**
     * Adds skipped row counts from one batch slice.
     *
     * @param count rows skipped in the slice
     */
    void addRowsSkipped(long count) {
        rowsSkipped += count;
    }
}
