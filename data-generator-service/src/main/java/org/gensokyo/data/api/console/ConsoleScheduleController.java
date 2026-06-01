/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.TaskScheduleUpsertRequest;
import org.gensokyo.data.api.console.dto.TaskScheduleView;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.task.TaskScheduleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Console API for cron-driven template run schedules.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@RestController
@RequestMapping("/api/console/schedules")
@RequiredArgsConstructor
public class ConsoleScheduleController {

    private final TaskScheduleService taskScheduleService;

    /**
     * Lists schedule rows, optionally filtered by template id.
     *
     * @param templateId optional filter
     * @return schedule views
     */
    @GetMapping
    public R<List<TaskScheduleView>> list(
            @RequestParam(name = "templateId", required = false) Long templateId) {
        if (templateId != null) {
            return R.ok(taskScheduleService.listByTemplateId(templateId));
        }
        return R.ok(taskScheduleService.listAll());
    }

    /**
     * @param id schedule id
     * @return schedule view
     */
    @GetMapping("/{id}")
    public R<TaskScheduleView> get(@NotNull @PathVariable Long id) {
        return R.ok(taskScheduleService.getById(id));
    }

    /**
     * Creates a schedule row.
     *
     * @param request upsert body
     * @return created view
     */
    @PostMapping
    public R<TaskScheduleView> create(@Valid @RequestBody TaskScheduleUpsertRequest request) {
        try {
            return R.ok(taskScheduleService.create(request));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Updates a schedule row.
     *
     * @param id      schedule id
     * @param request upsert body
     * @return updated view
     */
    @PutMapping("/{id}")
    public R<TaskScheduleView> update(
            @NotNull @PathVariable Long id, @Valid @RequestBody TaskScheduleUpsertRequest request) {
        try {
            return R.ok(taskScheduleService.update(id, request));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Deletes a schedule row.
     *
     * @param id schedule id
     * @return acknowledgement
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@NotNull @PathVariable Long id) {
        try {
            taskScheduleService.delete(id);
            return R.ok("Schedule deleted");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
