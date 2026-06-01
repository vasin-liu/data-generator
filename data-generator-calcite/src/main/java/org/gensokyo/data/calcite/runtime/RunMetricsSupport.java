/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Aggregates per-partition {@link RunMetrics} into a single run-level collector.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
final class RunMetricsSupport {

    private RunMetricsSupport() {
    }

    /**
     * Merges counters and warnings from {@code source} into {@code target}.
     *
     * @param target aggregate metrics
     * @param source partition or block metrics
     */
    static void mergeInto(RunMetrics target, RunMetrics source) {
        if (target == null || source == null) {
            return;
        }
        source.getRowsReadPerSource().forEach((name, count) -> target.addRead(name, count.intValue()));
        target.addRowsWritten((int) source.getRowsWritten());
        target.recordPeakRowsInMemory(source.getPeakRowsInMemory());
        for (int chunk = 0; chunk < source.getChunksProcessed(); chunk++) {
            target.incrementChunks();
        }
        source.getWarnings().forEach(target::addWarning);
        source.getSinkMetrics().forEach((key, metric) -> {
            target.recordSinkRowsOk(key, metric.getRowsOk());
            if (metric.getRowsFailed() > 0) {
                target.recordSinkRowsFailed(key, metric.getRowsFailed(), metric.getLastErrorSample());
            }
        });
    }
}
