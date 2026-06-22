/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.controller.TaskController;
import org.gensokyo.data.model.po.TaskExecutionPO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TaskExecutionRepository;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for structured V2 run report persistence.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class RunReportPersistenceTests {

    private static final long TEMPLATE_ID = 91001L;
    private static final long TEMPLATE_ID_FAIL = 91002L;
    private static final Pattern INSTANCE_ID_PATTERN = Pattern.compile("instanceId=(\\d+)");

    @Autowired
    private TaskController taskController;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @AfterEach
    void cleanup() {
        taskExecutionRepository.findAll().stream()
                .filter(row -> row.getTemplateId() != null
                        && (row.getTemplateId().equals(TEMPLATE_ID) || row.getTemplateId().equals(TEMPLATE_ID_FAIL)))
                .forEach(taskExecutionRepository::delete);
        templateRepository.findById(TEMPLATE_ID).ifPresent(templateRepository::delete);
        templateRepository.findById(TEMPLATE_ID_FAIL).ifPresent(templateRepository::delete);
    }

    @Test
    void v2RunPersistsStructuredReport() throws Exception {
        TemplatePO entity = new TemplatePO();
        entity.setId(TEMPLATE_ID);
        entity.setName("run-report-v2");
        entity.setContentYaml("""
                name: run-report-v2
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 3
                      step: 1
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<String> start = taskController.runById(entity.getId());
        assertThat(start.isSuccess()).isTrue();

        Long instanceId = extractInstanceId(start.getMessage());
        TaskExecutionSummary summary = awaitSuccess(instanceId);

        assertThat(summary.report()).isNotNull();
        assertThat(summary.report().sources()).isNotEmpty();
        assertThat(summary.report().sources().getFirst().name()).isEqualTo("input");
        assertThat(summary.report().sources().getFirst().rowsProcessed()).isEqualTo(3L);
        assertThat(summary.report().executionMode()).isNotBlank();

        TaskExecutionPO persisted = taskExecutionRepository.findByInstanceId(instanceId).orElseThrow();
        assertThat(persisted.getReportJson()).contains("\"sources\"");
        assertThat(persisted.getReportJson()).contains("input");
    }

    @Test
    void failingTransformPersistsStructuredErrorReachingJobDetail() throws Exception {
        TemplatePO entity = new TemplatePO();
        entity.setId(TEMPLATE_ID_FAIL);
        entity.setName("run-report-v2-fail");
        entity.setContentYaml("""
                name: run-report-v2-fail
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 3
                      step: 1
                transform:
                  type: mask
                  rules:
                    - column: value
                      strategy: rot13
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<String> start = taskController.runById(entity.getId());
        assertThat(start.isSuccess()).isTrue();

        Long instanceId = extractInstanceId(start.getMessage());
        TaskExecutionSummary summary = awaitFailed(instanceId);

        // The structured error reaches the job-detail carrier (JobExecutionDetail.execution IS this summary).
        assertThat(summary.report()).isNotNull();
        assertThat(summary.report().transformErrors()).isNotEmpty();
        var transformError = summary.report().transformErrors().getFirst();
        assertThat(transformError.operatorType()).isEqualTo("mask");
        assertThat(transformError.step()).isEqualTo("transformers[0]");

        TaskExecutionPO persisted = taskExecutionRepository.findByInstanceId(instanceId).orElseThrow();
        assertThat(persisted.getReportJson()).contains("transformErrors");
    }

    private static Long extractInstanceId(String message) {
        Matcher matcher = INSTANCE_ID_PATTERN.matcher(message);
        assertThat(matcher.find()).isTrue();
        return Long.valueOf(matcher.group(1));
    }

    private TaskExecutionSummary awaitSuccess(Long instanceId) throws InterruptedException {
        TaskExecutionSummary summary = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            summary = taskExecutionService.getByInstanceId(instanceId);
            if (TaskExecutionStatus.SUCCESS.name().equals(summary.status())) {
                return summary;
            }
            if (TaskExecutionStatus.FAILED.name().equals(summary.status())) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(200);
        }
        assertThat(summary).isNotNull();
        assertThat(summary.status()).isEqualTo(TaskExecutionStatus.SUCCESS.name());
        return summary;
    }

    private TaskExecutionSummary awaitFailed(Long instanceId) throws InterruptedException {
        TaskExecutionSummary summary = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            summary = taskExecutionService.getByInstanceId(instanceId);
            if (TaskExecutionStatus.FAILED.name().equals(summary.status())) {
                return summary;
            }
            if (TaskExecutionStatus.SUCCESS.name().equals(summary.status())) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(200);
        }
        assertThat(summary).isNotNull();
        assertThat(summary.status()).isEqualTo(TaskExecutionStatus.FAILED.name());
        return summary;
    }
}
