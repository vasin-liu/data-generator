/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration tests for {@code data.generator.v1-execution.enabled}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.v1-execution.enabled=false"
        }
)
@Import(TaskControllerV1ExecutionFlagTests.CapturingRunnerConfig.class)
class TaskControllerV1ExecutionFlagTests {

    @Autowired
    private TaskController taskController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CapturingTemplateV2Runner templateV2Runner;

    @Autowired
    private DataGeneratorProperties dataGeneratorProperties;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
        templateV2Runner.reset();
    }

    @Test
    void runByIdFailsWhenV1ExecutionDisabled() {
        TemplatePO entity = new TemplatePO();
        entity.setId(99001L);
        entity.setName("v1-flag-blocked");
        entity.setContentYaml("""
                name: v1-flag-blocked
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

        Assertions.assertFalse(dataGeneratorProperties.isV1ExecutionEnabled());

        R<String> result = taskController.runById(entity.getId());

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("V1 template execution is disabled"),
                () -> "message=" + result.getMessage());
    }

    @Test
    void runByIdStillRunsV2WhenV1ExecutionDisabled() throws InterruptedException {
        TemplatePO entity = new TemplatePO();
        entity.setId(99002L);
        entity.setName("v2-flag-allowed");
        entity.setContentYaml("""
                name: v2-flag-allowed
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
        templateRepository.saveAndFlush(entity);

        R<String> result = taskController.runById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(templateV2Runner.awaitInvocation(5, TimeUnit.SECONDS));
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
