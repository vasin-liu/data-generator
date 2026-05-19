/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Thrown when a Template V2 run exceeds a configured execution policy limit.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class ScaleLimitExceededException extends IllegalStateException {

    /**
     * Creates an exception describing which policy field was exceeded.
     *
     * @param policyField name of the exceeded policy field (e.g. {@code maxRowsInMemory})
     * @param limit configured limit value
     * @param actual observed value that exceeded the limit
     * @param stage pipeline stage where the limit was detected
     * @param sourceName source identifier related to the violation
     */
    public ScaleLimitExceededException(
            String policyField,
            long limit,
            long actual,
            String stage,
            String sourceName) {
        super("Execution policy limit exceeded: field=" + policyField
                + ", limit=" + limit
                + ", actual=" + actual
                + ", stage=" + stage
                + ", source=" + sourceName);
    }
}
