/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Per-sink write counters collected during Template V2 sink execution.
 * <p>
 * {@link #rowsRead} counts rows accepted into each sink write batch (per writer, per chunk).
 * {@link #rowsSkipped} counts rows intentionally not written (null upsert key, validation filter)
 * and must not be conflated with {@link #rowsFailed}.
 * </p>
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class SinkWriteMetric {

    private long rowsOk;
    private long rowsFailed;
    private long rowsRead;
    private long rowsUpserted;
    private long rowsSkipped;
    private String lastErrorSample;

    /**
     * Rows successfully written for this sink writer.
     *
     * @return successful row count
     */
    public long getRowsOk() {
        return rowsOk;
    }

    /**
     * Rows that failed to write for this sink writer.
     *
     * @return failed row count
     */
    public long getRowsFailed() {
        return rowsFailed;
    }

    /**
     * Rows accepted into this sink writer batch (per chunk in streaming/chunked runs).
     *
     * @return rows read into the sink stage for this writer
     */
    public long getRowsRead() {
        return rowsRead;
    }

    /**
     * Rows updated via upsert/merge for this sink writer.
     *
     * @return upsert row count
     */
    public long getRowsUpserted() {
        return rowsUpserted;
    }

    /**
     * Rows intentionally skipped before write (null upsert key, validation filter, etc.).
     *
     * @return skipped row count; distinct from {@link #getRowsFailed()}
     */
    public long getRowsSkipped() {
        return rowsSkipped;
    }

    /**
     * Truncated sample of the most recent write failure message.
     *
     * @return error sample, or {@code null} when no failure was recorded
     */
    public String getLastErrorSample() {
        return lastErrorSample;
    }

    /**
     * Records rows accepted into this sink write batch.
     *
     * @param count number of rows passed to the sink in this increment
     */
    void addRowsRead(long count) {
        rowsRead += count;
    }

    /**
     * Records successfully written rows.
     *
     * @param count number of rows written in this increment
     */
    void addRowsOk(long count) {
        rowsOk += count;
    }

    /**
     * Records rows intentionally skipped before write.
     *
     * @param count number of skipped rows in this increment
     */
    void addRowsSkipped(long count) {
        rowsSkipped += count;
    }

    /**
     * Records rows updated via upsert SQL during batch execution.
     *
     * @param count number of upserted rows in this increment
     */
    void addRowsUpserted(long count) {
        rowsUpserted += count;
    }

    /**
     * Records failed rows and the latest error sample.
     *
     * @param count number of rows that failed in this increment
     * @param errorSample error message sample (truncated to 500 characters)
     */
    void addRowsFailed(long count, String errorSample) {
        rowsFailed += count;
        lastErrorSample = truncateErrorSample(errorSample);
    }

    private static String truncateErrorSample(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}
