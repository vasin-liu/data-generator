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
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Embedded coordinator/worker loop that leases one queue row and executes it on the local service process.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedJobCoordinator {

    private final DistributedExecutionProperties distributedExecutionProperties;
    private final DistributedJobService distributedJobService;
    private final TaskExecutionService taskExecutionService;
    private final TemplateRepository templateRepository;
    private final org.gensokyo.data.yaml.YamlParser yamlParser;
    private final TemplateV2Runner templateV2Runner;
    private final RunReportCollector runReportCollector;
    private final DataGeneratorProperties dataGeneratorProperties;
    private final AuditService auditService;
    private final ObjectProvider<WorkflowRunControl> workflowRunControlProvider;

    /**
     * Polls queue rows and executes at most one leased job per iteration.
     */
    @Scheduled(fixedDelayString = "${pci.data.generator.distributed.poll-delay-ms:2000}")
    public void pollAndRun() {
        if (!distributedExecutionProperties.isEnabled()) {
            return;
        }
        String workerId = distributedExecutionProperties.getWorkerId();
        int leaseSeconds = distributedExecutionProperties.getLeaseSeconds();
        distributedJobService.leaseNext(workerId, leaseSeconds).ifPresent(lease -> runLease(workerId, leaseSeconds, lease));
    }

    private void runLease(String workerId, int leaseSeconds, DistributedJobLease lease) {
        Long jobId = lease.jobId();
        Long instanceId = lease.instanceId();
        try {
            distributedJobService.markRunning(jobId, workerId);
            distributedJobService.heartbeat(jobId, workerId, leaseSeconds);

            if (taskExecutionService.isCancelRequested(instanceId)) {
                taskExecutionService.markCancelled(instanceId);
                distributedJobService.markCancelled(jobId, workerId);
                return;
            }
            TemplateV2VO template = loadTemplate(lease);
            taskExecutionService.markRunning(instanceId);
            executeTrackedRun(workerId, leaseSeconds, lease, template);
        } catch (Exception e) {
            log.warn("Distributed coordinator failed to run job {} instance {}", jobId, instanceId, e);
            taskExecutionService.markFailed(instanceId, e.getMessage());
            distributedJobService.markFailed(jobId, workerId, e.getMessage());
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
            WorkflowRunContext.clear();
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
        TemplateV2Validator.validateGovernance(
                template, dataGeneratorProperties.getGovernance().isRejectPlaintextPasswordsInTemplates());
        return template;
    }
}

