/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import java.io.Serializable;

/**
 * Per-stage counters for a Template V2 run report.
 *
 * @param name           stage identifier (source name, transform name, or sink key)
 * @param rowsProcessed  rows read or written at this stage when known
 * @param durationMs     stage duration in milliseconds when measured
 * @param errorSample    truncated failure sample for sinks under continue-on-error
 * @param rowsOk         rows successfully written when partial-success metrics are collected
 * @param rowsFailed     rows that failed to write when partial-success metrics are collected
 * @param rowsRead       rows accepted into this sink write batch (per-sink, not global source total)
 * @param rowsUpserted   rows updated via upsert/merge SQL for JDBC sinks
 * @param rowsSkipped    rows intentionally not written (null upsert key, validation filter, etc.)
 * @author Gensokyo
 * @since 2026-05-29
 */
public record StageMetricVO(
        String name,
        Long rowsProcessed,
        Long durationMs,
        String errorSample,
        Long rowsOk,
        Long rowsFailed,
        Long rowsRead,
        Long rowsUpserted,
        Long rowsSkipped) implements Serializable {

    /**
     * Normalizes nullable extended sink counters for backward-compatible report deserialization.
     */
    public StageMetricVO {
        if (rowsRead == null) {
            rowsRead = 0L;
        }
        if (rowsUpserted == null) {
            rowsUpserted = 0L;
        }
        if (rowsSkipped == null) {
            rowsSkipped = 0L;
        }
    }

    /**
     * Back-compatible constructor for callers that predate extended sink counters (D-16).
     *
     * @param name          stage identifier
     * @param rowsProcessed rows read or written at this stage when known
     * @param durationMs    stage duration in milliseconds when measured
     * @param errorSample   truncated failure sample
     * @param rowsOk        rows successfully written
     * @param rowsFailed    rows that failed to write
     */
    public StageMetricVO(
            String name,
            Long rowsProcessed,
            Long durationMs,
            String errorSample,
            Long rowsOk,
            Long rowsFailed) {
        this(name, rowsProcessed, durationMs, errorSample, rowsOk, rowsFailed, null, null, null);
    }
}
