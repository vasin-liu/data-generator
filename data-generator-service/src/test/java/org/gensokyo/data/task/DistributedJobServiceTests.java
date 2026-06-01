/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.DistributedJobPO;
import org.gensokyo.data.repository.DistributedJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for distributed queue lease + heartbeat lifecycle.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class DistributedJobServiceTests {

    @Autowired
    private DistributedJobService distributedJobService;

    @Autowired
    private DistributedJobRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void leaseHeartbeatAndTerminalStatusPersist() {
        Long jobId = distributedJobService.enqueue(101L, 202L, 303L, "{\"phase\":\"c2\"}");

        DistributedJobLease lease = distributedJobService.leaseNext("worker-a", 30).orElseThrow();
        Assertions.assertEquals(jobId, lease.jobId());
        Assertions.assertEquals(303L, lease.instanceId());
        Assertions.assertEquals(202L, lease.templateId());

        distributedJobService.heartbeat(jobId, "worker-a", 30);
        distributedJobService.markRunning(jobId, "worker-a");
        distributedJobService.markSuccess(jobId, "worker-a");

        DistributedJobPO row = repository.findById(jobId).orElseThrow();
        Assertions.assertEquals(DistributedJobStatus.SUCCESS.name(), row.getStatus());
        Assertions.assertEquals("worker-a", row.getWorkerId());
        Assertions.assertEquals(1, row.getAttempts());
        Assertions.assertNotNull(row.getFinishedAt());
    }
}

