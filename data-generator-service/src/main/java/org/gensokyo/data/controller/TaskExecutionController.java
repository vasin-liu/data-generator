/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequiredArgsConstructor
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;

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
}
