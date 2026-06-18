/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

/**
 * Structured validation or governance failure for UDF registry operations.
 *
 * @param code    machine-readable error code (e.g. {@code UDF_GOVERNANCE_VIOLATION})
 * @param field   optional field path
 * @param message human-readable detail
 * @author Gensokyo
 * @since 2026-06-17
 */
public record UdfValidationError(String code, String field, String message) {

    /**
     * @return formatted message for logs and APIs
     */
    public String formatted() {
        if (field == null || field.isBlank()) {
            return code + ": " + message;
        }
        return code + " [" + field + "]: " + message;
    }
}
