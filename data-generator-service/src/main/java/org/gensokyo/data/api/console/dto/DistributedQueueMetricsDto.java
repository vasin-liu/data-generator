/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read-only distributed queue snapshot for the operator console.
 *
 * @param distributedEnabled      whether distributed mode is on
 * @param workerEnabled           whether a worker poller is active on this JVM
 * @param coordinatorPollEnabled  whether the embedded coordinator polls on this JVM
 * @param jobsByStatus            queue depth grouped by status
 * @param activeWorkers           workers with leased/running rows
 * @param collectedAt             snapshot timestamp
 * @author Gensokyo
 * @since 2026-06-01
 */
public record DistributedQueueMetricsDto(
        boolean distributedEnabled,
        boolean workerEnabled,
        boolean coordinatorPollEnabled,
        Map<String, Long> jobsByStatus,
        List<WorkerHealthDto> activeWorkers,
        Instant collectedAt) {
}
