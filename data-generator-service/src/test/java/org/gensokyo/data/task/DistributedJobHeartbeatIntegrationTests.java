/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.model.po.DistributedJobPO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.repository.DistributedJobRepository;
import org.gensokyo.data.repository.TaskExecutionRepository;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.util.RandomKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for execution-period distributed job heartbeat (Phase C2).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.distributed.enabled=true",
                "data.generator.distributed.lease-seconds=1",
                "data.generator.distributed.heartbeat-interval-ms=500"
        }
)
@Import(DistributedJobHeartbeatIntegrationTests.SlowRunnerTestConfig.class)
class DistributedJobHeartbeatIntegrationTests {

    @Autowired
    private DistributedJobService distributedJobService;

    @Autowired
    private DistributedJobLeaseRunner distributedJobLeaseRunner;

    @Autowired
    private DistributedJobRepository distributedJobRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @AfterEach
    void cleanup() {
        distributedJobRepository.deleteAll();
        taskExecutionRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    void heartbeatUpdatesDuringSlowRun() throws Exception {
        TemplatePO entity = new TemplatePO();
        entity.setId(99101L);
        entity.setName("heartbeat-slow-v2");
        entity.setContentYaml("""
                name: heartbeat-slow-v2
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

        Long instanceId = RandomKit.snowFlake().nextId();
        taskExecutionService.queueExecution(entity.getId(), entity.getName(), instanceId, "V2");
        Long jobId = distributedJobService.enqueue(null, entity.getId(), instanceId, null);
        DistributedJobLease lease = distributedJobService.leaseNext("heartbeat-worker", 1).orElseThrow();

        Set<Instant> heartbeatSnapshots = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> runFuture = executor.submit(
                    () -> distributedJobLeaseRunner.runLease("heartbeat-worker", 1, lease));
            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(8);
            while (!runFuture.isDone() && System.currentTimeMillis() < deadline) {
                distributedJobRepository.findById(jobId).ifPresent(row -> {
                    if (row.getLastHeartbeatAt() != null) {
                        heartbeatSnapshots.add(row.getLastHeartbeatAt());
                    }
                });
                Thread.sleep(100);
            }
            runFuture.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        DistributedJobPO finished = distributedJobRepository.findById(jobId).orElseThrow();
        Assertions.assertEquals(DistributedJobStatus.SUCCESS.name(), finished.getStatus());
        // Initial heartbeat plus at least two scheduled updates during the 2s slow run.
        Assertions.assertTrue(
                heartbeatSnapshots.size() >= 3,
                "expected multiple heartbeat timestamps, saw: " + heartbeatSnapshots.size());
    }

    @TestConfiguration
    static class SlowRunnerTestConfig {
        @Bean
        @Primary
        SlowTemplateV2Runner slowTemplateV2Runner() {
            return new SlowTemplateV2Runner();
        }
    }

    static class SlowTemplateV2Runner extends TemplateV2Runner {
        @Override
        public TemplateV2RunResult run(TemplateV2VO template) {
            try {
                Thread.sleep(2_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Slow runner interrupted", e);
            }
            return new TemplateV2RunResult(new RowSchema(), List.of(new Row(Map.of())));
        }
    }
}
