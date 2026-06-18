/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import java.util.List;

/**
 * Registry operation failure with structured error codes.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
public class UdfRegistryException extends RuntimeException {

    private final String code;
    private final List<UdfValidationError> errors;

    /**
     * @param code    primary error code
     * @param message detail message
     */
    public UdfRegistryException(String code, String message) {
        this(code, message, List.of(new UdfValidationError(code, null, message)));
    }

    /**
     * @param code    primary error code
     * @param message detail message
     * @param errors  structured errors
     */
    public UdfRegistryException(String code, String message, List<UdfValidationError> errors) {
        super(message);
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    /**
     * @return primary error code
     */
    public String code() {
        return code;
    }

    /**
     * @return structured errors (never null)
     */
    public List<UdfValidationError> errors() {
        return errors;
    }
}
