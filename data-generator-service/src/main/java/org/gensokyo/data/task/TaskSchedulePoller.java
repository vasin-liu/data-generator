/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.config.TaskScheduleProperties;
import org.gensokyo.data.controller.TaskController;
import org.gensokyo.data.model.po.TaskSchedulePO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Polls due {@code task_schedule} rows and starts published template runs.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = TaskScheduleProperties.PREFIX, name = "enabled", havingValue = "true")
public class TaskSchedulePoller {

    private final TaskScheduleProperties taskScheduleProperties;
    private final TaskScheduleService taskScheduleService;
    private final TaskController taskController;
    private final AuditService auditService;

    /**
     * Evaluates due schedules and triggers template runs.
     */
    @Scheduled(fixedDelayString = "${data.generator.schedule.poll-delay-ms:60000}")
    public void pollDueSchedules() {
        if (!taskScheduleProperties.isEnabled()) {
            return;
        }
        Instant now = Instant.now();
        List<TaskSchedulePO> due = taskScheduleService.findDue(now);
        if (due.isEmpty()) {
            return;
        }
        log.info("Task schedule poller found {} due row(s)", due.size());
        for (TaskSchedulePO schedule : due) {
            triggerOne(schedule, now);
        }
    }

    private void triggerOne(TaskSchedulePO schedule, Instant now) {
        Long scheduleId = schedule.getId();
        Long templateId = schedule.getTemplateId();
        Long lastInstanceId = null;
        try {
            TaskController.TemplateRunStartResult started = taskController.triggerScheduledRun(templateId);
            lastInstanceId = started.instanceId();
            auditService.record(
                    "TASK_SCHEDULE_TRIGGER",
                    "TASK_SCHEDULE",
                    String.valueOf(scheduleId),
                    Map.of(
                            "templateId", templateId,
                            "instanceId", started.instanceId(),
                            "cron", schedule.getCronExpression()));
            log.info(
                    "Scheduled run started for template {} instance {}",
                    templateId,
                    started.instanceId());
        } catch (Exception e) {
            log.warn(
                    "Scheduled run skipped or failed for schedule {} template {}: {}",
                    scheduleId,
                    templateId,
                    e.getMessage());
            auditService.record(
                    "TASK_SCHEDULE_TRIGGER_FAILED",
                    "TASK_SCHEDULE",
                    String.valueOf(scheduleId),
                    Map.of("templateId", templateId, "error", String.valueOf(e.getMessage())));
        } finally {
            // Always advance next fire time to avoid tight loops on persistent errors.
            taskScheduleService.markTriggered(scheduleId, now, lastInstanceId);
        }
    }
}
