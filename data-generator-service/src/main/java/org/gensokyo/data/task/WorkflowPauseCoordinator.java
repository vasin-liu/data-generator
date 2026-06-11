/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
/**
 * In-memory manual pause gates keyed by run {@code instanceId}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Component
public class WorkflowPauseCoordinator {

    private final Map<Long, CountDownLatch> resumeLatches = new ConcurrentHashMap<>();

    /**
     * Blocks the workflow thread until {@link #resume(Long)} or interrupt.
     *
     * @param instanceId run instance id
     * @throws InterruptedException when interrupted or cancelled
     */
    public void awaitResume(Long instanceId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        resumeLatches.put(instanceId, latch);
        try {
            latch.await();
        } finally {
            resumeLatches.remove(instanceId);
        }
    }

    /**
     * @param instanceId run instance id
     * @return {@code true} when a latch was released
     */
    public boolean resume(Long instanceId) {
        CountDownLatch latch = resumeLatches.get(instanceId);
        if (latch == null) {
            return false;
        }
        latch.countDown();
        return true;
    }

    /**
     * @param instanceId run instance id
     * @return whether a manual pause is active
     */
    public boolean isPaused(Long instanceId) {
        return resumeLatches.containsKey(instanceId);
    }

    /**
     * Best-effort wake for cancel while paused.
     *
     * @param instanceId run instance id
     */
    public void cancelPause(Long instanceId) {
        CountDownLatch latch = resumeLatches.remove(instanceId);
        if (latch != null) {
            latch.countDown();
        }
    }
}
