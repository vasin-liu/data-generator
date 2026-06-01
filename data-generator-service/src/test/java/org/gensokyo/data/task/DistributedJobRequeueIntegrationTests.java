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
 * Integration tests for distributed job failure requeue policy (Phase C2).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class DistributedJobRequeueIntegrationTests {

    @Autowired
    private DistributedJobService distributedJobService;

    @Autowired
    private DistributedJobRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void markFailedRequeuesWhenAttemptsRemain() {
        Long jobId = distributedJobService.enqueue(1L, 2L, 3L, null);
        distributedJobService.leaseNext("w1", 30);
        distributedJobService.markRunning(jobId, "w1");
        distributedJobService.markFailedWithRetryPolicy(jobId, "w1", "boom", 3, true);

        DistributedJobPO row = repository.findById(jobId).orElseThrow();
        Assertions.assertEquals(DistributedJobStatus.QUEUED.name(), row.getStatus());
        Assertions.assertNull(row.getWorkerId());
        Assertions.assertEquals("boom", row.getErrorMessage());
    }

    @Test
    void markFailedTerminalWhenMaxAttemptsExceeded() {
        Long jobId = distributedJobService.enqueue(10L, 20L, 30L, null);

        distributedJobService.leaseNext("w1", 30);
        distributedJobService.markRunning(jobId, "w1");
        distributedJobService.markFailedWithRetryPolicy(jobId, "w1", "fail-1", 3, true);

        distributedJobService.leaseNext("w2", 30);
        distributedJobService.markRunning(jobId, "w2");
        distributedJobService.markFailedWithRetryPolicy(jobId, "w2", "fail-2", 3, true);

        distributedJobService.leaseNext("w3", 30);
        distributedJobService.markRunning(jobId, "w3");
        distributedJobService.markFailedWithRetryPolicy(jobId, "w3", "fail-3", 3, true);

        DistributedJobPO row = repository.findById(jobId).orElseThrow();
        Assertions.assertEquals(DistributedJobStatus.FAILED.name(), row.getStatus());
        Assertions.assertEquals("w3", row.getWorkerId());
        Assertions.assertEquals(3, row.getAttempts());
    }
}
