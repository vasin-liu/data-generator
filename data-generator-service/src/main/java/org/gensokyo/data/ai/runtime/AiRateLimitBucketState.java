/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import java.util.ArrayDeque;

/**
 * Mutable bucket state shared by in-process and JDBC-backed rate limiters.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
final class AiRateLimitBucketState {

    private long lastCallMs;
    private final ArrayDeque<Long> window = new ArrayDeque<>();

    /**
     * @param now    current epoch millis
     * @param policy resolved throttle policy
     * @return milliseconds to wait before the call is allowed
     */
    long waitMs(long now, AiRateLimitPolicy policy) {
        long wait = 0L;
        if (policy.minIntervalMs() > 0L && lastCallMs > 0L) {
            wait = Math.max(wait, policy.minIntervalMs() - (now - lastCallMs));
        }
        if (policy.requestsPerMinute() > 0) {
            prune(now - policy.windowMs());
            if (window.size() >= policy.requestsPerMinute()) {
                Long oldest = window.peekFirst();
                if (oldest != null) {
                    wait = Math.max(wait, policy.windowMs() - (now - oldest));
                }
            }
        }
        return wait;
    }

    /**
     * Records a successful acquire at {@code now}.
     *
     * @param now    current epoch millis
     * @param policy resolved throttle policy
     */
    void record(long now, AiRateLimitPolicy policy) {
        lastCallMs = now;
        if (policy.requestsPerMinute() > 0) {
            prune(now - policy.windowMs());
            window.addLast(now);
        }
    }

    long lastCallMs() {
        return lastCallMs;
    }

    void setLastCallMs(long lastCallMs) {
        this.lastCallMs = lastCallMs;
    }

    ArrayDeque<Long> window() {
        return window;
    }

    private void prune(long cutoff) {
        while (!window.isEmpty()) {
            Long head = window.peekFirst();
            if (head == null || head >= cutoff) {
                break;
            }
            window.removeFirst();
        }
    }
}
