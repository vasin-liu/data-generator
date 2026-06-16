/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process throttle for remote AI provider calls keyed by provider bucket.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public class AiRateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Blocks until the call is allowed under the resolved policy.
     *
     * @param key    limiter bucket key
     * @param policy throttle policy
     */
    public void acquire(String key, AiRateLimitPolicy policy) {
        if (policy == null || !policy.enabled()) {
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
        synchronized (bucket) {
            while (true) {
                long now = System.currentTimeMillis();
                long waitMs = bucket.waitMs(now, policy);
                if (waitMs <= 0L) {
                    bucket.record(now, policy);
                    return;
                }
                sleepQuietly(waitMs);
            }
        }
    }

    private static void sleepQuietly(long waitMs) {
        try {
            Thread.sleep(waitMs);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI rate-limit wait interrupted", ex);
        }
    }

    private static final class Bucket {
        private long lastCallMs;
        private final ArrayDeque<Long> window = new ArrayDeque<>();

        private long waitMs(long now, AiRateLimitPolicy policy) {
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

        private void record(long now, AiRateLimitPolicy policy) {
            lastCallMs = now;
            if (policy.requestsPerMinute() > 0) {
                prune(now - policy.windowMs());
                window.addLast(now);
            }
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
}
