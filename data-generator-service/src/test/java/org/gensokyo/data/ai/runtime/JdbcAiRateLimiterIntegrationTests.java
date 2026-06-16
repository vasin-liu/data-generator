/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.repository.AiRateLimitStateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Integration tests for {@link JdbcAiRateLimiter} cross-instance coordination.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.ai-runtime.distributed-rate-limit-enabled=true"
        }
)
class JdbcAiRateLimiterIntegrationTests {

    @Autowired
    private AiRateLimitStateRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void coordinatesMinIntervalAcrossLimiterInstances() throws Exception {
        JdbcAiRateLimiter first = new JdbcAiRateLimiter(repository, transactionTemplate);
        JdbcAiRateLimiter second = new JdbcAiRateLimiter(repository, transactionTemplate);
        AiRateLimitPolicy policy = new AiRateLimitPolicy(120L, 0);

        CountDownLatch started = new CountDownLatch(1);
        AtomicLong secondElapsedMs = new AtomicLong();

        Thread worker = new Thread(() -> {
            try {
                started.await(5, TimeUnit.SECONDS);
                long start = System.currentTimeMillis();
                second.acquire("OPENAI:shared", policy);
                secondElapsedMs.set(System.currentTimeMillis() - start);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        worker.start();

        first.acquire("OPENAI:shared", policy);
        started.countDown();
        worker.join(5_000L);

        Assertions.assertTrue(secondElapsedMs.get() >= 80L, "expected cross-instance wait, elapsed=" + secondElapsedMs.get());
    }
}
