/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TaskExecutionPO;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.repository.TaskExecutionRepository;
import org.gensokyo.data.util.RandomKit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Persists and queries template run lifecycle for the operator job center.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    private static final List<String> ACTIVE_STATUSES = List.of(
            TaskExecutionStatus.QUEUED.name(),
            TaskExecutionStatus.RUNNING.name(),
            TaskExecutionStatus.PAUSED.name());

    private final TaskExecutionRepository repository;

    /**
     * @param templateId template id
     * @return true when a QUEUED or RUNNING row exists
     */
    public boolean isRunning(Long templateId) {
        return repository.existsByTemplateIdAndStatusIn(templateId, ACTIVE_STATUSES);
    }

    /**
     * Inserts a QUEUED execution row before async work starts.
     *
     * @param templateId      template id
     * @param templateName    template name
     * @param instanceId      snowflake instance id
     * @param definitionKind  V1 or V2
     * @return persisted row id
     */
    @Transactional
    public Long queueExecution(Long templateId, String templateName, Long instanceId, String definitionKind) {
        return queueExecution(templateId, templateName, instanceId, definitionKind, null, null, null);
    }

    /**
     * Inserts a QUEUED execution row with optional lineage snapshots.
     *
     * @param templateId           template id
     * @param templateName         template name
     * @param instanceId           snowflake instance id
     * @param definitionKind       V1 or V2
     * @param templateVersion      content hash snapshot
     * @param pluginSetJson        plugin registry snapshot JSON
     * @param datasourceConfigHash datasource registry hash
     * @return persisted row id
     */
    @Transactional
    public Long queueExecution(
            Long templateId,
            String templateName,
            Long instanceId,
            String definitionKind,
            String templateVersion,
            String pluginSetJson,
            String datasourceConfigHash) {
        Instant now = Instant.now();
        TaskExecutionPO row = new TaskExecutionPO();
        row.setId(RandomKit.snowFlake().nextId());
        row.setTemplateId(templateId);
        row.setTemplateName(templateName);
        row.setInstanceId(instanceId);
        row.setDefinitionKind(definitionKind);
        row.setStatus(TaskExecutionStatus.QUEUED.name());
        row.setQueuedAt(now);
        row.setCancelRequested(Boolean.FALSE);
        row.setTemplateVersion(templateVersion);
        row.setPluginSetJson(pluginSetJson);
        row.setDatasourceConfigHash(datasourceConfigHash);
        repository.saveAndFlush(row);
        return row.getId();
    }

    /**
     * @param instanceId run instance id
     */
    @Transactional
    public void markRunning(Long instanceId) {
        update(instanceId, row -> {
            row.setStatus(TaskExecutionStatus.RUNNING.name());
            if (row.getStartedAt() == null) {
                row.setStartedAt(Instant.now());
            }
        });
    }

    /**
     * @param instanceId  run instance id
     * @param rowCount    rows processed
     * @param metricsJson optional V2 metrics JSON
     */
    @Transactional
    public void markSuccess(Long instanceId, Long rowCount, String metricsJson) {
        markSuccess(instanceId, rowCount, metricsJson, null);
    }

    /**
     * @param instanceId  run instance id
     * @param rowCount    rows processed
     * @param metricsJson optional V2 metrics JSON
     * @param reportJson  optional structured V2 run report JSON
     */
    @Transactional
    public void markSuccess(Long instanceId, Long rowCount, String metricsJson, String reportJson) {
        update(instanceId, row -> {
            row.setStatus(TaskExecutionStatus.SUCCESS.name());
            row.setFinishedAt(Instant.now());
            row.setRowCount(rowCount);
            row.setMetricsJson(metricsJson);
            row.setReportJson(reportJson);
            row.setErrorMessage(null);
        });
    }

    /**
     * @param instanceId   run instance id
     * @param errorMessage failure detail
     */
    /**
     * @param instanceId run instance id
     */
    @Transactional
    public void markPaused(Long instanceId) {
        update(instanceId, row -> row.setStatus(TaskExecutionStatus.PAUSED.name()));
    }

    /**
     * @param instanceId run instance id
     * @return {@code true} when cancel was requested
     */
    public boolean isCancelRequested(Long instanceId) {
        return repository.findByInstanceId(instanceId)
                .map(row -> Boolean.TRUE.equals(row.getCancelRequested()))
                .orElse(false);
    }

    /**
     * Best-effort cancel for QUEUED, RUNNING, or PAUSED runs.
     *
     * @param instanceId run instance id
     */
    @Transactional
    public void requestCancel(Long instanceId) {
        update(instanceId, row -> {
            String status = row.getStatus();
            if (TaskExecutionStatus.SUCCESS.name().equals(status)
                    || TaskExecutionStatus.FAILED.name().equals(status)
                    || TaskExecutionStatus.CANCELLED.name().equals(status)) {
                throw new IllegalArgumentException("Cannot cancel finished execution: " + instanceId);
            }
            row.setCancelRequested(Boolean.TRUE);
            if (TaskExecutionStatus.QUEUED.name().equals(status)) {
                row.setStatus(TaskExecutionStatus.CANCELLED.name());
                row.setFinishedAt(Instant.now());
            }
        });
    }

    /**
     * @param instanceId run instance id
     */
    @Transactional
    public void markCancelled(Long instanceId) {
        update(instanceId, row -> {
            row.setStatus(TaskExecutionStatus.CANCELLED.name());
            row.setFinishedAt(Instant.now());
        });
    }

    @Transactional
    public void markFailed(Long instanceId, String errorMessage) {
        update(instanceId, row -> {
            row.setStatus(TaskExecutionStatus.FAILED.name());
            row.setFinishedAt(Instant.now());
            String message = errorMessage;
            if (message != null && message.length() > 4000) {
                message = message.substring(0, 4000);
            }
            row.setErrorMessage(message);
        });
    }

    /**
     * @param templateId optional filter
     * @return summaries newest first (by finished/queued time)
     */
    public List<TaskExecutionSummary> list(Long templateId) {
        List<TaskExecutionPO> rows = templateId == null
                ? repository.findAll().stream()
                        .sorted((a, b) -> sortKey(b).compareTo(sortKey(a)))
                        .toList()
                : repository.findByTemplateIdOrderByFinishedAtDesc(templateId);
        return rows.stream().map(this::toSummary).toList();
    }

    /**
     * @param instanceId run instance id
     * @return summary when found
     */
    public TaskExecutionSummary getByInstanceId(Long instanceId) {
        return repository.findByInstanceId(instanceId)
                .map(this::toSummary)
                .orElseThrow(() -> new IllegalArgumentException("Unknown execution: " + instanceId));
    }

    private void update(Long instanceId, java.util.function.Consumer<TaskExecutionPO> consumer) {
        TaskExecutionPO row = repository.findByInstanceId(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown execution: " + instanceId));
        consumer.accept(row);
        repository.saveAndFlush(row);
    }

    private static Instant sortKey(TaskExecutionPO row) {
        if (row.getFinishedAt() != null) {
            return row.getFinishedAt();
        }
        if (row.getStartedAt() != null) {
            return row.getStartedAt();
        }
        return Objects.requireNonNullElse(row.getQueuedAt(), Instant.EPOCH);
    }

    private TaskExecutionSummary toSummary(TaskExecutionPO row) {
        return new TaskExecutionSummary(
                row.getId(),
                row.getTemplateId(),
                row.getTemplateName(),
                row.getInstanceId(),
                row.getDefinitionKind(),
                row.getStatus(),
                row.getQueuedAt(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getRowCount(),
                row.getErrorMessage(),
                row.getMetricsJson(),
                parseReport(row.getReportJson()));
    }

    private static RunReportVO parseReport(String reportJson) {
        if (reportJson == null || reportJson.isBlank()) {
            return null;
        }
        return TemplateJsonCodec.read(reportJson, RunReportVO.class);
    }
}
