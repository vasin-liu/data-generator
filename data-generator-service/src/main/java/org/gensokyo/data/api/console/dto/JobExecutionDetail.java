/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.task.TaskExecutionSummary;

/**
 * Console job detail with optional distributed queue and partition metrics.
 *
 * @param execution         task execution row
 * @param distributedJob    linked distributed queue row when present
 * @param partitionMetrics  partition counters from V2 metrics JSON when present
 * @author Gensokyo
 * @since 2026-06-01
 */
public record JobExecutionDetail(
        TaskExecutionSummary execution,
        DistributedJobView distributedJob,
        PartitionRunMetrics partitionMetrics) {
}
