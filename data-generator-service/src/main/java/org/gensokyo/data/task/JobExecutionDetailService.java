/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.JobExecutionDetail;
import org.springframework.stereotype.Service;

/**
 * Composes console job detail with distributed queue and partition metrics.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Service
@RequiredArgsConstructor
public class JobExecutionDetailService {

    private final TaskExecutionService taskExecutionService;
    private final DistributedJobMetricsService distributedJobMetricsService;

    /**
     * @param instanceId run instance id
     * @return execution detail for the console job page
     */
    public JobExecutionDetail getDetail(Long instanceId) {
        TaskExecutionSummary execution = taskExecutionService.getByInstanceId(instanceId);
        return new JobExecutionDetail(
                execution,
                distributedJobMetricsService.findJobByInstanceId(instanceId),
                distributedJobMetricsService.parsePartitionMetrics(execution.metricsJson()));
    }
}
