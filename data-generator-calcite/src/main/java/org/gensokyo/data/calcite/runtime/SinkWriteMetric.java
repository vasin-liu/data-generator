/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Per-sink write counters collected when {@code CONTINUE_ON_ERROR} is active.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class SinkWriteMetric {

    private long rowsOk;
    private long rowsFailed;
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
     * Truncated sample of the most recent write failure message.
     *
     * @return error sample, or {@code null} when no failure was recorded
     */
    public String getLastErrorSample() {
        return lastErrorSample;
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
