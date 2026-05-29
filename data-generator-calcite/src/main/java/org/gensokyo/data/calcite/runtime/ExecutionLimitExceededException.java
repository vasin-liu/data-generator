/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Thrown when a Template V2 run exceeds {@code maxTotalRows} and fail-fast is enabled.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class ExecutionLimitExceededException extends IllegalStateException {

    /**
     * Creates an exception describing the template-level row cap violation.
     *
     * @param templateName template identifier from {@code TemplateV2VO#getName()}
     * @param limit configured {@code maxTotalRows} value
     * @param actual cumulative rows processed when the limit was exceeded
     */
    public ExecutionLimitExceededException(String templateName, int limit, long actual) {
        super("Execution limit exceeded: template=" + templateName
                + ", maxTotalRows=" + limit
                + ", actual=" + actual);
    }
}
