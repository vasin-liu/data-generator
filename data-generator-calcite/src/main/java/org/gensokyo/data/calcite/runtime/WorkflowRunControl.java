/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Hooks for operator cancel and manual workflow pause during a template run.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public interface WorkflowRunControl {

    /** No-op control for unit tests and preview paths. */
    WorkflowRunControl NO_OP = new WorkflowRunControl() {
    };

    /**
     * @param instanceId current run instance id
     * @return {@code true} when the operator requested cancellation
     */
    default boolean isCancelRequested(Long instanceId) {
        return false;
    }

    /**
     * Blocks until resume or cancel when a manual pause step is reached.
     *
     * @param instanceId current run instance id
     * @throws InterruptedException when the waiting thread is interrupted
     */
    default void awaitManualPause(Long instanceId) throws InterruptedException {
        // default: no manual pause support
    }

    /**
     * @param instanceId current run instance id
     * @throws InterruptedException when cancelled while running
     */
    default void checkCancel(Long instanceId) throws InterruptedException {
        if (isCancelRequested(instanceId)) {
            throw new InterruptedException("Run cancelled: instanceId=" + instanceId);
        }
    }
}
