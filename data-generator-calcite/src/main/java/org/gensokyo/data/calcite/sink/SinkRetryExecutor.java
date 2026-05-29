/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;

import java.util.function.Supplier;

/**
 * Shared retry helper for sink write operations driven by {@link SinkExecutionPolicyVO}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class SinkRetryExecutor {

    private SinkRetryExecutor() {
    }

    /**
     * Runs a sink write operation with retry when configured on the policy.
     *
     * @param policy sink execution policy (may be null)
     * @param operation write operation to execute
     */
    public static void run(SinkExecutionPolicyVO policy, Runnable operation) {
        runWithResult(policy, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Runs a sink write operation with retry when configured on the policy.
     *
     * @param policy sink execution policy (may be null)
     * @param operation write operation to execute
     * @param <T> result type
     * @return result from the successful attempt
     */
    public static <T> T runWithResult(SinkExecutionPolicyVO policy, Supplier<T> operation) {
        int maxAttempts = resolveMaxAttempts(policy);
        long backoffMs = resolveBackoffMs(policy);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (attempt >= maxAttempts) {
                    break;
                }
                if (backoffMs > 0L) {
                    sleepQuietly(backoffMs);
                }
            }
        }
        throw lastFailure;
    }

    /**
     * Resolves the maximum number of attempts for a sink write, including the first try.
     *
     * @param policy sink execution policy (may be null)
     * @return at least one attempt
     */
    static int resolveMaxAttempts(SinkExecutionPolicyVO policy) {
        if (policy == null || policy.getMaxRetries() == null) {
            return 1;
        }
        return Math.max(1, policy.getMaxRetries());
    }

    /**
     * Resolves backoff between retry attempts in milliseconds.
     *
     * @param policy sink execution policy (may be null)
     * @return non-negative backoff
     */
    static long resolveBackoffMs(SinkExecutionPolicyVO policy) {
        if (policy == null || policy.getRetryBackoffMs() == null) {
            return 0L;
        }
        return Math.max(0L, policy.getRetryBackoffMs());
    }

    private static void sleepQuietly(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sink retry interrupted", interrupted);
        }
    }
}
