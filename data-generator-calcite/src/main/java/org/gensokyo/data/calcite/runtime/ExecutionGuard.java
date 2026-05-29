/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.model.v2.TemplateV2VO;

/**
 * Shared execution-policy guards applied across Template V2 pipelines.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
final class ExecutionGuard {

    private ExecutionGuard() {
    }

    /**
     * Fails the run when cumulative rows read exceed {@code maxTotalRows} and fail-fast is enabled.
     *
     * @param template template being executed
     * @param policy   resolved execution policy
     * @param metrics  run metrics with cumulative row counts
     */
    static void checkMaxTotalRows(TemplateV2VO template, EffectiveExecutionPolicy policy, RunMetrics metrics) {
        Integer maxTotalRows = policy.maxTotalRows();
        if (maxTotalRows == null) {
            return;
        }
        long actual = metrics.getTotalRowsRead();
        // Compare after each increment so oversized chunks fail once the cap is crossed.
        if (actual > maxTotalRows && policy.failOnLimitExceeded()) {
            throw new ExecutionLimitExceededException(template.getName(), maxTotalRows, actual);
        }
    }
}
