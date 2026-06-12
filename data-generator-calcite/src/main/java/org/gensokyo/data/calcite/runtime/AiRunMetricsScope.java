/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Thread-local holder that binds the active {@link RunMetrics} while sources are materialized.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
public final class AiRunMetricsScope {

    private static final ThreadLocal<RunMetrics> CURRENT = new ThreadLocal<>();

    private AiRunMetricsScope() {
    }

    /**
     * Binds the active run metrics collector for the current thread.
     *
     * @param metrics metrics instance for the in-flight run
     */
    public static void bind(RunMetrics metrics) {
        CURRENT.set(metrics);
    }

    /**
     * Clears the bound metrics for the current thread.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Returns the bound metrics for the current thread when present.
     *
     * @return active metrics, or {@code null} when not bound
     */
    public static RunMetrics current() {
        return CURRENT.get();
    }
}
