/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.calcite.runtime.WorkflowRunControl;

/**
 * Bridges {@link TaskExecutionService} and {@link WorkflowPauseCoordinator} to calcite workflow runs.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@RequiredArgsConstructor
public class TaskWorkflowRunControl implements WorkflowRunControl {

    private final TaskExecutionService taskExecutionService;
    private final WorkflowPauseCoordinator pauseCoordinator;

    @Override
    public boolean isCancelRequested(Long instanceId) {
        return instanceId != null && taskExecutionService.isCancelRequested(instanceId);
    }

    @Override
    public void awaitManualPause(Long instanceId) throws InterruptedException {
        awaitManualPause(instanceId, null);
    }

    @Override
    public void awaitManualPause(Long instanceId, String pauseReason) throws InterruptedException {
        if (instanceId == null) {
            return;
        }
        taskExecutionService.markPaused(instanceId, pauseReason);
        try {
            pauseCoordinator.awaitResume(instanceId);
        } finally {
            if (!isCancelRequested(instanceId)) {
                taskExecutionService.markRunning(instanceId);
            }
        }
        checkCancel(instanceId);
    }
}
