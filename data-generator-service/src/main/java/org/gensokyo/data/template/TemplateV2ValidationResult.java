/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import java.util.List;

/**
 * Outcome of control-plane validation for a Template V2 draft.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public class TemplateV2ValidationResult {

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    /**
     * Creates a validation result.
     *
     * @param valid    {@code true} when no errors were recorded
     * @param errors   validation errors (never {@code null})
     * @param warnings non-fatal advisories (never {@code null})
     */
    public TemplateV2ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
        this.warnings = List.copyOf(warnings);
    }

    /**
     * @return {@code true} when the draft passed validation
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return immutable list of validation errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * @return immutable list of validation warnings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Builds an invalid result with a single error message.
     *
     * @param message error text
     * @return invalid result
     */
    static TemplateV2ValidationResult invalid(String message) {
        return new TemplateV2ValidationResult(false, List.of(message), List.of());
    }

    /**
     * Builds a result from mutable error and warning accumulators.
     *
     * @param errors   collected errors
     * @param warnings collected warnings
     * @return result with {@code valid} when {@code errors} is empty
     */
    static TemplateV2ValidationResult from(List<String> errors, List<String> warnings) {
        return new TemplateV2ValidationResult(
                errors.isEmpty(),
                errors.isEmpty() ? List.of() : List.copyOf(errors),
                warnings.isEmpty() ? List.of() : List.copyOf(warnings));
    }
}
