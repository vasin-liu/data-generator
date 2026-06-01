/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.controller.TaskController;
import org.gensokyo.data.model.po.DistributedJobPO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.DistributedJobRepository;
import org.gensokyo.data.repository.TaskExecutionRepository;
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
 * Integration tests for coordinator enqueue-only + worker execution split (Phase C2 staging).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.distributed.enabled=true",
                "data.generator.distributed.worker-enabled=true",
                "data.generator.distributed.coordinator-poll-enabled=false"
        }
)
@Import(DistributedSplitRoleIntegrationTests.CapturingRunnerTestConfig.class)
class DistributedSplitRoleIntegrationTests {

    @Autowired
    private TaskController taskController;

    @Autowired(required = false)
    private DistributedJobCoordinator distributedJobCoordinator;

    @Autowired
    private DistributedJobWorker distributedJobWorker;

    @Autowired
    private DistributedJobRepository distributedJobRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private CapturingTemplateV2Runner templateV2Runner;

    @AfterEach
    void cleanup() {
        distributedJobRepository.deleteAll();
        taskExecutionRepository.deleteAll();
        templateRepository.deleteAll();
        templateV2Runner.reset();
    }

    @Test
    void coordinatorDoesNotPollWorkerExecutes() throws InterruptedException {
        Assertions.assertNull(
                distributedJobCoordinator,
                "coordinator bean must be absent when coordinator-poll-enabled=false");

        TemplatePO entity = new TemplatePO();
        entity.setId(88301L);
        entity.setName("split-role-v2");
        entity.setContentYaml("""
                name: split-role-v2
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 2
                      step: 1
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sinkExecutionPolicy:
                  mode: CONTINUE_ON_ERROR
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<String> start = taskController.runById(entity.getId());
        Assertions.assertTrue(start.isSuccess());
        Assertions.assertFalse(templateV2Runner.awaitInvocation(500, TimeUnit.MILLISECONDS));

        DistributedJobPO queued = distributedJobRepository.findAll().getFirst();
        Assertions.assertEquals(DistributedJobStatus.QUEUED.name(), queued.getStatus());

        distributedJobWorker.pollAndRun();

        Assertions.assertTrue(templateV2Runner.awaitInvocation(5, TimeUnit.SECONDS));

        DistributedJobPO finished = distributedJobRepository.findById(queued.getId()).orElseThrow();
        Assertions.assertEquals(DistributedJobStatus.SUCCESS.name(), finished.getStatus());

        TaskExecutionSummary execution = taskExecutionService.getByInstanceId(queued.getInstanceId());
        Assertions.assertEquals(TaskExecutionStatus.SUCCESS.name(), execution.status());
    }

    @TestConfiguration
    static class CapturingRunnerTestConfig {
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
