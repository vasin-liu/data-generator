/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.TaskScheduleUpsertRequest;
import org.gensokyo.data.api.console.dto.TaskScheduleView;
import org.gensokyo.data.model.po.TaskSchedulePO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TaskScheduleRepository;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateLifecycleService;
import org.gensokyo.data.util.RandomKit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * CRUD and due-row queries for cron-driven template run schedules.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Service
@RequiredArgsConstructor
public class TaskScheduleService {

    private final TaskScheduleRepository repository;
    private final TemplateRepository templateRepository;
    private final TemplateLifecycleService templateLifecycleService;

    /**
     * @return all schedules newest first
     */
    @Transactional(readOnly = true)
    public List<TaskScheduleView> listAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> {
                    Instant left = a.getCreatedAt() == null ? Instant.EPOCH : a.getCreatedAt();
                    Instant right = b.getCreatedAt() == null ? Instant.EPOCH : b.getCreatedAt();
                    return right.compareTo(left);
                })
                .map(this::toView)
                .toList();
    }

    /**
     * @param templateId template id filter
     * @return schedules for the template
     */
    @Transactional(readOnly = true)
    public List<TaskScheduleView> listByTemplateId(Long templateId) {
        return repository.findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * @param id schedule id
     * @return schedule view
     */
    @Transactional(readOnly = true)
    public TaskScheduleView getById(Long id) {
        return toView(requireSchedule(id));
    }

    /**
     * Creates a schedule row with an initial {@code nextTriggerAt}.
     *
     * @param request upsert payload
     * @return persisted view
     */
    @Transactional
    public TaskScheduleView create(TaskScheduleUpsertRequest request) {
        requireTemplate(request.templateId());
        Instant now = Instant.now();
        TaskSchedulePO row = new TaskSchedulePO();
        row.setId(RandomKit.snowFlake().nextId());
        applyUpsert(row, request, now);
        row.setCreatedAt(now);
        repository.saveAndFlush(row);
        return toView(row);
    }

    /**
     * Updates an existing schedule row and recomputes {@code nextTriggerAt}.
     *
     * @param id      schedule id
     * @param request upsert payload
     * @return updated view
     */
    @Transactional
    public TaskScheduleView update(Long id, TaskScheduleUpsertRequest request) {
        requireTemplate(request.templateId());
        TaskSchedulePO row = requireSchedule(id);
        applyUpsert(row, request, Instant.now());
        repository.saveAndFlush(row);
        return toView(row);
    }

    /**
     * @param id schedule id
     */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Unknown task schedule: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * @param now current timestamp
     * @return enabled schedules due for triggering
     */
    @Transactional(readOnly = true)
    public List<TaskSchedulePO> findDue(Instant now) {
        return repository.findDue(now);
    }

    /**
     * Advances trigger timestamps and captures the last started instance id.
     *
     * @param id             schedule id
     * @param triggeredAt    trigger timestamp
     * @param lastInstanceId started instance id, or null when trigger failed/skipped
     */
    @Transactional
    public void markTriggered(Long id, Instant triggeredAt, Long lastInstanceId) {
        TaskSchedulePO row = requireSchedule(id);
        row.setLastTriggeredAt(triggeredAt);
        row.setLastInstanceId(lastInstanceId);
        if (Boolean.TRUE.equals(row.getEnabled())) {
            row.setNextTriggerAt(TaskScheduleSupport.nextTriggerAfter(row.getCronExpression(), triggeredAt));
        }
        row.setUpdatedAt(triggeredAt);
        repository.saveAndFlush(row);
    }

    private void applyUpsert(TaskSchedulePO row, TaskScheduleUpsertRequest request, Instant now) {
        row.setTemplateId(request.templateId());
        row.setCronExpression(request.cronExpression().trim());
        row.setEnabled(request.enabled() == null ? Boolean.TRUE : request.enabled());
        row.setDescription(request.description());
        row.setUpdatedAt(now);
        if (Boolean.TRUE.equals(row.getEnabled())) {
            // Fire time strictly after "now" so a row created mid-minute is not immediately re-due.
            row.setNextTriggerAt(TaskScheduleSupport.nextTriggerAfter(row.getCronExpression(), now));
        } else {
            row.setNextTriggerAt(null);
        }
    }

    private TaskSchedulePO requireSchedule(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown task schedule: " + id));
    }

    private void requireTemplate(Long templateId) {
        TemplatePO template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template does not exist: " + templateId));
        if (Boolean.TRUE.equals(template.getArchived())) {
            throw new IllegalArgumentException("Template is archived: " + templateId);
        }
        // Scheduled runs follow the same publish gate as manual production runs.
        templateLifecycleService.requirePublishedForTaskRun(template);
    }

    private TaskScheduleView toView(TaskSchedulePO row) {
        return new TaskScheduleView(
                row.getId(),
                row.getTemplateId(),
                row.getCronExpression(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getDescription(),
                row.getLastTriggeredAt(),
                row.getLastInstanceId(),
                row.getNextTriggerAt());
    }

    /**
     * Computes the next cron fire time after now (for console preview).
     *
     * @param cronExpression Spring six-field cron text
     * @return next trigger instant
     * @throws IllegalArgumentException when the expression is invalid or has no next fire
     */
    public Instant previewNextTrigger(String cronExpression) {
        return TaskScheduleSupport.nextTriggerAfter(cronExpression, Instant.now());
    }
}
