/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.AiRateLimitStatePO;
import org.gensokyo.data.repository.AiRateLimitStateRepository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed throttle that coordinates AI call pacing across JVMs via row locks.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public final class JdbcAiRateLimiter implements AiRateLimiter {

    private final AiRateLimitStateRepository repository;
    private final TransactionTemplate transactionTemplate;

    /**
     * @param repository          persisted bucket store
     * @param transactionTemplate short transactions for acquire attempts
     */
    public JdbcAiRateLimiter(AiRateLimitStateRepository repository, TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void acquire(String key, AiRateLimitPolicy policy) {
        if (policy == null || !policy.enabled()) {
            return;
        }
        while (true) {
            Long waitMs = transactionTemplate.execute(status -> {
                AiRateLimitStatePO row = repository.findLockedByLimiterKey(key)
                        .orElseGet(() -> newRow(key));
                AiRateLimitBucketState bucket = toBucket(row);
                long now = System.currentTimeMillis();
                long wait = bucket.waitMs(now, policy);
                if (wait <= 0L) {
                    bucket.record(now, policy);
                    applyBucket(row, bucket);
                    repository.save(row);
                }
                return wait;
            });
            if (waitMs == null || waitMs <= 0L) {
                return;
            }
            sleepQuietly(waitMs);
        }
    }

    private static AiRateLimitStatePO newRow(String key) {
        AiRateLimitStatePO row = new AiRateLimitStatePO();
        row.setLimiterKey(key);
        row.setLastCallMs(0L);
        row.setWindowTimestampsJson("[]");
        return row;
    }

    private static AiRateLimitBucketState toBucket(AiRateLimitStatePO row) {
        AiRateLimitBucketState bucket = new AiRateLimitBucketState();
        bucket.setLastCallMs(row.getLastCallMs());
        ArrayDeque<Long> window = bucket.window();
        for (Long timestamp : readWindow(row.getWindowTimestampsJson())) {
            window.addLast(timestamp);
        }
        return bucket;
    }

    private static void applyBucket(AiRateLimitStatePO row, AiRateLimitBucketState bucket) {
        row.setLastCallMs(bucket.lastCallMs());
        row.setWindowTimestampsJson(writeWindow(bucket.window()));
    }

    private static List<Long> readWindow(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Long[] values = TemplateJsonCodec.read(json, Long[].class);
            if (values == null || values.length == 0) {
                return List.of();
            }
            List<Long> timestamps = new ArrayList<>(values.length);
            for (Long value : values) {
                if (value != null) {
                    timestamps.add(value);
                }
            }
            return timestamps;
        }
        catch (RuntimeException ex) {
            return List.of();
        }
    }

    private static String writeWindow(ArrayDeque<Long> window) {
        return TemplateJsonCodec.write(new ArrayList<>(window));
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
