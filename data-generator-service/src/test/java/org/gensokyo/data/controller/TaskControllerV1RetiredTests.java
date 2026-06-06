/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.task.TaskExecutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for retired V1 template execution on {@link TaskController}.
 *
 * @author Gensokyo &lt;liuweixing@pcitech.com&gt;
 * @since 2026-06-06
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.v1-execution.enabled=false"
        }
)
@Import(TaskControllerV1RetiredTests.CapturingRunnerConfig.class)
class TaskControllerV1RetiredTests {

    @Autowired
    private TaskController taskController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private CapturingTemplateV2Runner templateV2Runner;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
        templateV2Runner.reset();
    }

    /**
     * Ensures {@code GET /task/runById} refuses V1 templates before pipeline start.
     */
    @Test
    void runById_rejectsV1Template() {
        TemplatePO entity = persistV1Template();

        R<String> result = taskController.runById(entity.getId());

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("V1"),
                () -> "message=" + result.getMessage());
        Assertions.assertTrue(result.getMessage().contains("no longer supported"),
                () -> "message=" + result.getMessage());
        Assertions.assertTrue(taskExecutionService.list(entity.getId()).isEmpty());
    }

    /**
     * Ensures {@code POST /task/run} refuses V1 templates before pipeline start.
     */
    @Test
    void postRunById_rejectsV1Template() {
        TemplatePO entity = persistV1Template();

        R<String> result = taskController.postRunById(entity.getId());

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("V1"),
                () -> "message=" + result.getMessage());
        Assertions.assertTrue(taskExecutionService.list(entity.getId()).isEmpty());
    }

    /**
     * Ensures V2 runs still succeed when V1 execution is retired.
     */
    @Test
    void runById_stillRunsV2Template() throws InterruptedException {
        TemplatePO entity = persistV2Template();

        R<String> result = taskController.runById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(templateV2Runner.awaitInvocation(5, TimeUnit.SECONDS));
    }

    /**
     * V1 rejection is unconditional: {@code v1-execution.enabled=true} must not revive V1 runs.
     */
    @Nested
    @SpringBootTest(
            classes = DataGeneratorApplication.class,
            properties = {
                    "spring.config.location=classpath:/application-phase7-test.yaml",
                    "data.generator.v1-execution.enabled=true"
            }
    )
    @Import(CapturingRunnerConfig.class)
    class WhenV1ExecutionFlagEnabled {

        @Autowired
        private TaskController taskController;

        @Autowired
        private TemplateRepository templateRepository;

        @Autowired
        private TaskExecutionService taskExecutionService;

        @AfterEach
        void tearDown() {
            templateRepository.deleteAll();
        }

        /**
         * Ensures the legacy flag cannot re-enable V1 task runs.
         */
        @Test
        void runById_stillRejectsV1Template() {
            TemplatePO entity = new TemplatePO();
            entity.setId(99103L);
            entity.setName("v1-flag-ignored");
            entity.setContentYaml("""
                    name: v1-flag-ignored
                    iterator:
                      type: number
                      from: 1
                      to: 1
                    fields:
                      - name: value
                        stages:
                          - type: MAPPING
                            mapping:
                              value: "#{iterator.value}"
                    output:
                      writers:
                        - type: console
                    """);
            templateRepository.saveAndFlush(entity);

            R<String> result = taskController.runById(entity.getId());

            Assertions.assertFalse(result.isSuccess());
            Assertions.assertTrue(result.getMessage().contains("no longer supported"),
                    () -> "message=" + result.getMessage());
            Assertions.assertTrue(taskExecutionService.list(entity.getId()).isEmpty());
        }
    }

    private TemplatePO persistV1Template() {
        TemplatePO entity = new TemplatePO();
        entity.setId(99101L);
        entity.setName("v1-retired");
        entity.setContentYaml("""
                name: v1-retired
                iterator:
                  type: number
                  from: 1
                  to: 1
                fields:
                  - name: value
                    stages:
                      - type: MAPPING
                        mapping:
                          value: "#{iterator.value}"
                output:
                  writers:
                    - type: console
                """);
        return templateRepository.saveAndFlush(entity);
    }

    private TemplatePO persistV2Template() {
        TemplatePO entity = new TemplatePO();
        entity.setId(99102L);
        entity.setName("v2-after-v1-retirement");
        entity.setContentYaml("""
                name: v2-after-v1-retirement
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 2
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sink:
                  writers:
                    - type: console
                """);
        return templateRepository.saveAndFlush(entity);
    }

    @TestConfiguration
    static class CapturingRunnerConfig {

        @Bean
        @Primary
        CapturingTemplateV2Runner capturingTemplateV2Runner() {
            return new CapturingTemplateV2Runner();
        }
    }

    static class CapturingTemplateV2Runner extends TemplateV2Runner {
        private volatile CountDownLatch latch = new CountDownLatch(1);

        @Override
        public TemplateV2RunResult run(TemplateV2VO template) {
            latch.countDown();
            return new TemplateV2RunResult(new RowSchema(), List.of(new Row(Map.of())));
        }

        boolean awaitInvocation(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        void reset() {
            latch = new CountDownLatch(1);
        }
    }
}
