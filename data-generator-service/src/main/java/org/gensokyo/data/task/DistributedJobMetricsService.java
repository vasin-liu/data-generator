/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.DistributedJobView;
import org.gensokyo.data.api.console.dto.DistributedQueueMetricsDto;
import org.gensokyo.data.api.console.dto.PartitionRunMetrics;
import org.gensokyo.data.api.console.dto.WorkerHealthDto;
import org.gensokyo.data.config.DistributedExecutionProperties;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.DistributedJobPO;
import org.gensokyo.data.repository.DistributedJobRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregations for distributed queue and partition run metrics.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Service
@RequiredArgsConstructor
public class DistributedJobMetricsService {

    private final DistributedExecutionProperties distributedExecutionProperties;
    private final DistributedJobRepository distributedJobRepository;

    /**
     * @return queue depth and active worker snapshot
     */
    public DistributedQueueMetricsDto queueMetrics() {
        Map<String, Long> jobsByStatus = new LinkedHashMap<>();
        for (Object[] row : distributedJobRepository.countGroupedByStatus()) {
            jobsByStatus.put(String.valueOf(row[0]), (Long) row[1]);
        }
        List<WorkerHealthDto> activeWorkers = distributedJobRepository.countActiveJobsByWorker().stream()
                .map(row -> new WorkerHealthDto(String.valueOf(row[0]), (Long) row[1]))
                .toList();
        return new DistributedQueueMetricsDto(
                distributedExecutionProperties.isEnabled(),
                distributedExecutionProperties.isWorkerEnabled(),
                distributedExecutionProperties.isCoordinatorPollEnabled(),
                jobsByStatus,
                activeWorkers,
                Instant.now());
    }

    /**
     * @param instanceId run instance id
     * @return distributed queue view when a row exists
     */
    public DistributedJobView findJobByInstanceId(Long instanceId) {
        return distributedJobRepository
                .findFirstByInstanceIdOrderByQueuedAtDesc(instanceId)
                .map(this::toView)
                .orElse(null);
    }

    /**
     * @param metricsJson serialized {@link org.gensokyo.data.calcite.runtime.RunMetrics}
     * @return partition counters when present in JSON
     */
    public PartitionRunMetrics parsePartitionMetrics(String metricsJson) {
        if (metricsJson == null || metricsJson.isBlank()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = TemplateJsonCodec.read(metricsJson, Map.class);
        Object configured = payload.get("configuredPartitions");
        Object executed = payload.get("executedPartitions");
        if (configured == null && executed == null) {
            return null;
        }
        int configuredPartitions = configured instanceof Number number ? number.intValue() : 0;
        int executedPartitions = executed instanceof Number number ? number.intValue() : 0;
        if (configuredPartitions <= 0 && executedPartitions <= 0) {
            return null;
        }
        return new PartitionRunMetrics(configuredPartitions, executedPartitions);
    }

    private DistributedJobView toView(DistributedJobPO row) {
        return new DistributedJobView(
                row.getId(),
                row.getStatus(),
                row.getWorkerId(),
                row.getLeaseUntil(),
                row.getAttempts(),
                row.getQueuedAt(),
                row.getFinishedAt());
    }
}
