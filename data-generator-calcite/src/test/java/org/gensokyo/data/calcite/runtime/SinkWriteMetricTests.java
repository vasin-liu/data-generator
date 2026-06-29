/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SinkWriteMetric} extended sink counters (RW-04, D-16).
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
class SinkWriteMetricTests {

    /**
     * Extended counters accumulate independently from failure counts.
     */
    @Test
    void accumulatesExtendedSinkCounters() {
        SinkWriteMetric metric = new SinkWriteMetric();
        metric.addRowsOk(10);
        metric.addRowsUpserted(3);
        metric.addRowsSkipped(1);
        metric.addRowsRead(11);
        metric.addRowsFailed(0, null);

        assertThat(metric.getRowsOk()).isEqualTo(10L);
        assertThat(metric.getRowsUpserted()).isEqualTo(3L);
        assertThat(metric.getRowsSkipped()).isEqualTo(1L);
        assertThat(metric.getRowsRead()).isEqualTo(11L);
        assertThat(metric.getRowsFailed()).isZero();
    }
}
