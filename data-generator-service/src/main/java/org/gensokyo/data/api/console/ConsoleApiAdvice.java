/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.udf.UdfRegistryException;
import org.gensokyo.data.udf.UdfValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Maps console API exceptions to {@link R} failure responses.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@Slf4j
@RestControllerAdvice(basePackages = "org.gensokyo.data.api.console")
public class ConsoleApiAdvice {

    /**
     * @param ex client error (unknown id, validation)
     * @return failure envelope with HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> badRequest(IllegalArgumentException ex) {
        return R.fail(ex.getMessage());
    }

    /**
     * Maps structured UDF registry failures to HTTP 400 with their code and field-level violations (D-12).
     *
     * @param ex registry/governance failure carrying a stable code and validation errors
     * @return failure envelope whose {@code data} exposes the code + violations for console rendering
     */
    @ExceptionHandler(UdfRegistryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<UdfErrorPayload> udfRegistryError(UdfRegistryException ex) {
        return R.fail(ex.getMessage(), new UdfErrorPayload(ex.code(), ex.errors()));
    }

    /**
     * @param ex unexpected failure
     * @return failure envelope with HTTP 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> serverError(Exception ex) {
        log.error("Console API error", ex);
        return R.fail(ex.getMessage() != null ? ex.getMessage() : "Internal error");
    }

    /**
     * Failure body for UDF registry errors: the stable {@code code} plus field-level {@code violations}.
     *
     * @param code       stable error code (e.g. {@code UDF_NOT_FOUND}, {@code UDF_GOVERNANCE_VIOLATION})
     * @param violations field-level validation errors (never {@code null})
     */
    public record UdfErrorPayload(String code, List<UdfValidationError> violations) {
    }
}
