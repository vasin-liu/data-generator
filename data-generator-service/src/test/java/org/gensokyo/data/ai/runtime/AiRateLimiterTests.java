/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiRateLimiter}.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
class AiRateLimiterTests {

    @Test
    void minIntervalMsBlocksBackToBackCalls() {
        AiRateLimiter limiter = new AiRateLimiter();
        AiRateLimitPolicy policy = new AiRateLimitPolicy(80L, 0);

        long start = System.currentTimeMillis();
        limiter.acquire("OLLAMA:test", policy);
        limiter.acquire("OLLAMA:test", policy);
        long elapsed = System.currentTimeMillis() - start;

        Assertions.assertTrue(elapsed >= 70L, "expected throttle wait, elapsed=" + elapsed);
    }

    @Test
    void requestsPerMinuteAllowsBurstUpToLimitWithoutWaiting() {
        AiRateLimiter limiter = new AiRateLimiter();
        AiRateLimitPolicy policy = new AiRateLimitPolicy(0L, 2);

        long start = System.currentTimeMillis();
        limiter.acquire("OPENAI:burst", policy);
        limiter.acquire("OPENAI:burst", policy);
        long elapsed = System.currentTimeMillis() - start;

        Assertions.assertTrue(elapsed < 30L, "first RPM window should not wait, elapsed=" + elapsed);
    }

    @Test
    void disabledPolicyDoesNotBlock() {
        AiRateLimiter limiter = new AiRateLimiter();
        AiRateLimitPolicy policy = new AiRateLimitPolicy(0L, 0);

        long start = System.currentTimeMillis();
        limiter.acquire("INLINE:default", policy);
        limiter.acquire("INLINE:default", policy);
        long elapsed = System.currentTimeMillis() - start;

        Assertions.assertTrue(elapsed < 30L, "disabled policy should not wait, elapsed=" + elapsed);
    }
}
