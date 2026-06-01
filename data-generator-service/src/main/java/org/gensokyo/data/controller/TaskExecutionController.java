/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import jakarta.validation.constraints.NotNull;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.gensokyo.data.task.WorkflowPauseCoordinator;
import org.springframework.context.annotation.Lazy;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Task execution history and POST run entry points for the operator console.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/task")
@Validated
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;
    private final WorkflowPauseCoordinator workflowPauseCoordinator;
    private final TaskController taskController;

    /**
     * @param taskExecutionService     execution persistence
     * @param workflowPauseCoordinator manual pause gates
     * @param taskController           run entry (lazy to break circular dependency)
     */
    public TaskExecutionController(
            TaskExecutionService taskExecutionService,
            WorkflowPauseCoordinator workflowPauseCoordinator,
            @Lazy TaskController taskController) {
        this.taskExecutionService = taskExecutionService;
        this.workflowPauseCoordinator = workflowPauseCoordinator;
        this.taskController = taskController;
    }

    /**
     * Lists execution history.
     *
     * @param templateId optional template filter
     * @return execution summaries
     */
    @GetMapping("/executions")
    public R<List<TaskExecutionSummary>> listExecutions(
            @RequestParam(required = false) Long templateId) {
        return R.ok(taskExecutionService.list(templateId));
    }

    /**
     * Returns one execution by instance id.
     *
     * @param instanceId run instance id
     * @return execution summary
     */
    @GetMapping("/executions/{instanceId}")
    public R<TaskExecutionSummary> getExecution(@NotNull @PathVariable Long instanceId) {
        try {
            return R.ok(taskExecutionService.getByInstanceId(instanceId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Best-effort cancel for QUEUED, RUNNING, or PAUSED executions.
     *
     * @param instanceId run instance id
     * @return acknowledgement
     */
    @PostMapping("/executions/{instanceId}/cancel")
    public R<String> cancel(@NotNull @PathVariable Long instanceId) {
        try {
            taskExecutionService.requestCancel(instanceId);
            workflowPauseCoordinator.cancelPause(instanceId);
            return R.ok("Cancel requested");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Resumes a workflow run blocked on a manual pause step.
     *
     * @param instanceId run instance id
     * @return acknowledgement
     */
    @PostMapping("/executions/{instanceId}/resume")
    public R<String> resume(@NotNull @PathVariable Long instanceId) {
        if (!workflowPauseCoordinator.resume(instanceId)) {
            return R.fail("No manual pause is active for instance: " + instanceId);
        }
        return R.ok("Resume signalled");
    }

    /**
     * Starts a new run for the same template as a prior execution.
     *
     * @param instanceId prior run instance id
     * @return start message from {@link TaskController}
     */
    @PostMapping("/executions/{instanceId}/retry")
    public R<String> retry(@NotNull @PathVariable Long instanceId) {
        TaskExecutionSummary prior = taskExecutionService.getByInstanceId(instanceId);
        return taskController.postRunById(prior.templateId());
    }
}
