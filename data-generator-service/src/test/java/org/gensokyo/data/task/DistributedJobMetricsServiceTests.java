/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.api.console.dto.DistributedQueueMetricsDto;
import org.gensokyo.data.api.console.dto.PartitionRunMetrics;
import org.gensokyo.data.repository.DistributedJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {@link DistributedJobMetricsService}.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class DistributedJobMetricsServiceTests {

    @Autowired
    private DistributedJobService distributedJobService;

    @Autowired
    private DistributedJobMetricsService distributedJobMetricsService;

    @Autowired
    private DistributedJobRepository distributedJobRepository;

    @AfterEach
    void cleanup() {
        distributedJobRepository.deleteAll();
    }

    @Test
    void queueMetricsCountsEnqueuedRows() {
        distributedJobService.enqueue(1L, 2L, 3L, null);
        distributedJobService.enqueue(4L, 5L, 6L, null);

        DistributedQueueMetricsDto metrics = distributedJobMetricsService.queueMetrics();

        Assertions.assertEquals(2L, metrics.jobsByStatus().get(DistributedJobStatus.QUEUED.name()));
    }

    @Test
    void parsePartitionMetricsReadsCountersFromJson() {
        String metricsJson =
                """
                {"executionMode":"BATCH","configuredPartitions":4,"executedPartitions":3}
                """;

        PartitionRunMetrics partitionMetrics = distributedJobMetricsService.parsePartitionMetrics(metricsJson);

        Assertions.assertNotNull(partitionMetrics);
        Assertions.assertEquals(4, partitionMetrics.configuredPartitions());
        Assertions.assertEquals(3, partitionMetrics.executedPartitions());
    }
}
