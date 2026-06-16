/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process throttle for remote AI provider calls keyed by provider bucket.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public final class InMemoryAiRateLimiter implements AiRateLimiter {

    private final ConcurrentHashMap<String, AiRateLimitBucketState> buckets = new ConcurrentHashMap<>();

    @Override
    public void acquire(String key, AiRateLimitPolicy policy) {
        if (policy == null || !policy.enabled()) {
            return;
        }
        AiRateLimitBucketState bucket = buckets.computeIfAbsent(key, ignored -> new AiRateLimitBucketState());
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
}
