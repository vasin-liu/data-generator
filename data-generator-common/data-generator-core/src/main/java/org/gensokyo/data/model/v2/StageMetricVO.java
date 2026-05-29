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
 * @author Gensokyo
 * @since 2026-05-29
 */
public record StageMetricVO(
        String name,
        Long rowsProcessed,
        Long durationMs,
        String errorSample) implements Serializable {
}
