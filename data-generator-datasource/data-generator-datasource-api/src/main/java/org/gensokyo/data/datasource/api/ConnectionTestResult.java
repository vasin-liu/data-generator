/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import java.util.Map;
import java.util.Objects;

/**
 * Outcome of {@link ConnectionCatalog#test(ConnectionTestRequest)} (D-18, D-20).
 * Details maps must not contain secret values.
 *
 * @param success {@code true} when connectivity succeeded
 * @param message operator-facing summary suitable for console display
 * @param details optional structured hints (host reachability, driver class, etc.) without secrets
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public record ConnectionTestResult(
        boolean success,
        String message,
        Map<String, Object> details) {

    /**
     * Compact constructor normalizing optional details.
     */
    public ConnectionTestResult {
        Objects.requireNonNull(message, "message");
        if (details != null) {
            details = Map.copyOf(details);
        }
    }

    /**
     * @param message success summary
     * @return successful test result without structured details
     */
    public static ConnectionTestResult ok(String message) {
        return new ConnectionTestResult(true, message, null);
    }

    /**
     * @param message failure summary for operator action
     * @return failed test result without structured details
     */
    public static ConnectionTestResult fail(String message) {
        return new ConnectionTestResult(false, message, null);
    }

    /**
     * @param message success summary
     * @param details non-secret structured hints
     * @return successful test result with details
     */
    public static ConnectionTestResult ok(String message, Map<String, Object> details) {
        return new ConnectionTestResult(true, message, details);
    }

    /**
     * @param message failure summary
     * @param details non-secret structured hints
     * @return failed test result with details
     */
    public static ConnectionTestResult fail(String message, Map<String, Object> details) {
        return new ConnectionTestResult(false, message, details);
    }
}
