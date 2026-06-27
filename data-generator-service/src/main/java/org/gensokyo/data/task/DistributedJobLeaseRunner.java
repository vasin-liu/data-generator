/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.WorkflowRunContext;
import org.gensokyo.data.calcite.runtime.WorkflowRunControl;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.config.DistributedExecutionProperties;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.template.TemplateLifecycleStatus;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.yaml.YamlParser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Executes one leased distributed queue row (shared by embedded coordinator and standalone workers).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedJobLeaseRunner {

    private final DistributedJobService distributedJobService;
    private final DistributedExecutionProperties distributedExecutionProperties;
    private final TaskExecutionService taskExecutionService;
    private final TemplateRepository templateRepository;
    private final YamlParser yamlParser;
    private final TemplateV2Runner templateV2Runner;
    private final RunReportCollector runReportCollector;
    private final DataGeneratorProperties dataGeneratorProperties;
    private final AuditService auditService;
    private final ObjectProvider<WorkflowRunControl> workflowRunControlProvider;
    private final ConnectionCatalog connectionCatalog;

    /**
     * Runs one leased queue row through V2 execution and terminal status updates.
     *
     * @param workerId     worker identity holding the lease
     * @param leaseSeconds lease ttl for heartbeat extension
     * @param lease        leased queue payload
     */
    public void runLease(String workerId, int leaseSeconds, DistributedJobLease lease) {
        Long jobId = lease.jobId();
        Long instanceId = lease.instanceId();
        TemplateV2VO template = null;
        long startedAtMs = System.currentTimeMillis();
        try {
            distributedJobService.markRunning(jobId, workerId);
            distributedJobService.heartbeat(jobId, workerId, leaseSeconds);

            if (taskExecutionService.isCancelRequested(instanceId)) {
                taskExecutionService.markCancelled(instanceId);
                distributedJobService.markCancelled(jobId, workerId);
                return;
            }
            template = loadTemplate(lease);
            taskExecutionService.markRunning(instanceId);
            taskExecutionService.captureConnectionSnapshot(instanceId, template, connectionCatalog);
            executeTrackedRun(workerId, leaseSeconds, lease, template);
        } catch (Exception e) {
            log.warn("Distributed worker failed to run job {} instance {}", jobId, instanceId, e);
            String reportJson = buildFailureReportJson(template, e, System.currentTimeMillis() - startedAtMs);
            taskExecutionService.markFailed(instanceId, e.getMessage(), reportJson);
            distributedJobService.markFailedWithRetryPolicy(
                    jobId,
                    workerId,
                    e.getMessage(),
                    distributedExecutionProperties.getMaxAttempts(),
                    distributedExecutionProperties.isRequeueOnFailure());
            auditService.record(
                    "TASK_RUN_FAILED",
                    "TASK",
                    String.valueOf(instanceId),
                    Map.of("error", String.valueOf(e.getMessage()), "distributedJobId", jobId));
        }
    }

    private void executeTrackedRun(String workerId, int leaseSeconds, DistributedJobLease lease, TemplateV2VO template) {
        Long instanceId = lease.instanceId();
        Long jobId = lease.jobId();
        WorkflowRunControl control = workflowRunControlProvider.getIfAvailable(() -> WorkflowRunControl.NO_OP);
        WorkflowRunContext.bind(instanceId, control);
        long startedAtMs = System.currentTimeMillis();
        ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        distributedJobService.heartbeat(jobId, workerId, leaseSeconds);
                    } catch (Exception ignored) {
                        // Lease lost — run will fail on next ownership check
                    }
                },
                distributedExecutionProperties.resolvedHeartbeatIntervalMs(),
                distributedExecutionProperties.resolvedHeartbeatIntervalMs(),
                TimeUnit.MILLISECONDS);
        try {
            TemplateV2RunResult result = templateV2Runner.run(template);
            distributedJobService.heartbeat(jobId, workerId, leaseSeconds);
            if (taskExecutionService.isCancelRequested(instanceId)) {
                taskExecutionService.markCancelled(instanceId);
                distributedJobService.markCancelled(jobId, workerId);
                return;
            }
            long durationMs = System.currentTimeMillis() - startedAtMs;
            long rowCount = 0L;
            String metricsJson = null;
            String reportJson = null;
            if (result.getMetrics() != null) {
                rowCount = result.getMetrics().getTotalRowsRead();
                metricsJson = TemplateJsonCodec.write(result.getMetrics());
                RunReportVO report = runReportCollector.collect(template, result, durationMs);
                if (report != null) {
                    reportJson = TemplateJsonCodec.write(report);
                }
            } else if (result.getRows() != null) {
                rowCount = result.getRows().size();
            }
            taskExecutionService.markSuccess(instanceId, rowCount, metricsJson, reportJson);
            distributedJobService.markSuccess(jobId, workerId);
            auditService.record(
                    "TASK_RUN_SUCCESS",
                    "TASK",
                    String.valueOf(instanceId),
                    Map.of("rowCount", rowCount, "distributedJobId", jobId));
        } finally {
            heartbeatTask.cancel(true);
            heartbeatExecutor.shutdownNow();
            WorkflowRunContext.clear();
        }
    }

    private String buildFailureReportJson(TemplateV2VO template, Throwable error, long durationMs) {
        try {
            RunReportVO report = runReportCollector.collectFailure(template, error, durationMs);
            return report == null ? null : TemplateJsonCodec.write(report);
        } catch (Exception ignored) {
            // Failure-report enrichment is best-effort; never mask the original run failure.
            return null;
        }
    }

    private TemplateV2VO loadTemplate(DistributedJobLease lease) {
        TaskExecutionSummary summary = taskExecutionService.getByInstanceId(lease.instanceId());
        TemplatePO entity = templateRepository.findById(summary.templateId())
                .orElseThrow(() -> new IllegalArgumentException("Template does not exist: " + summary.templateId()));
        TemplateV2DraftVO draft = yamlParser.parse(entity.getContentYaml(), TemplateV2DraftVO.class);
        TemplateV2VO template = TemplateV2Normalizer.normalize(draft);
        template.setId(entity.getId());
        template.setInstanceId(lease.instanceId());
        TemplateV2Validator.validate(template);
        boolean grandfatherRun = TemplateLifecycleStatus.PUBLISHED.name().equalsIgnoreCase(
                entity.getStatus() == null ? TemplateLifecycleStatus.PUBLISHED.name() : entity.getStatus());
        TemplateV2Validator.validateGovernance(
                template,
                dataGeneratorProperties.getGovernance().isRejectPlaintextPasswordsInTemplates(),
                dataGeneratorProperties.getGovernance(),
                connectionCatalog,
                grandfatherRun);
        return template;
    }
}
