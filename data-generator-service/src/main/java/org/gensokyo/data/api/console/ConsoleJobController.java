/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.gensokyo.data.task.WorkflowPauseCoordinator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Task execution history for the React job center.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class ConsoleJobController {

    private final TaskExecutionService taskExecutionService;
    private final WorkflowPauseCoordinator workflowPauseCoordinator;

    /**
     * Lists execution history rows for the console job center.
     *
     * @param templateId optional filter; when absent or blank, returns all executions
     * @return execution summaries newest first
     */
    @GetMapping
    public R<List<TaskExecutionSummary>> list(
            @RequestParam(name = "templateId", required = false) String templateId) {
        Long parsedTemplateId;
        if (templateId == null || templateId.isBlank()) {
            parsedTemplateId = null;
        } else {
            parsedTemplateId = Long.valueOf(templateId);
        }
        return R.ok(taskExecutionService.list(parsedTemplateId));
    }

    /**
     * Fetches a single execution row by its run instance id.
     *
     * @param instanceId run instance id
     * @return single execution row
     */
    @GetMapping("/{instanceId}")
    public R<TaskExecutionSummary> get(@NotNull @PathVariable Long instanceId) {
        return R.ok(taskExecutionService.getByInstanceId(instanceId));
    }

    /**
     * @param instanceId run instance id
     * @return acknowledgement
     */
    @PostMapping("/{instanceId}/cancel")
    public R<String> cancel(@NotNull @PathVariable Long instanceId) {
        taskExecutionService.requestCancel(instanceId);
        workflowPauseCoordinator.cancelPause(instanceId);
        return R.ok("Cancel requested");
    }

    /**
     * @param instanceId run instance id
     * @return acknowledgement
     */
    @PostMapping("/{instanceId}/resume")
    public R<String> resume(@NotNull @PathVariable Long instanceId) {
        if (!workflowPauseCoordinator.resume(instanceId)) {
            return R.fail("No manual pause is active for instance: " + instanceId);
        }
        return R.ok("Resume signalled");
    }
}
