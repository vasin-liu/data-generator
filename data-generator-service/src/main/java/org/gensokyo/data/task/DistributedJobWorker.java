/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.config.DistributedExecutionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Standalone worker poller for distributed queue execution (Phase C2 multi-node).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pci.data.generator.distributed", name = "worker-enabled", havingValue = "true")
public class DistributedJobWorker {

    private final DistributedExecutionProperties distributedExecutionProperties;
    private final DistributedJobService distributedJobService;
    private final DistributedJobLeaseRunner distributedJobLeaseRunner;

    /**
     * Polls queue rows and executes at most one leased job per iteration.
     */
    @Scheduled(fixedDelayString = "${pci.data.generator.distributed.poll-delay-ms:2000}")
    public void pollAndRun() {
        if (!distributedExecutionProperties.isEnabled()) {
            return;
        }
        String workerId = distributedExecutionProperties.getWorkerId();
        int leaseSeconds = distributedExecutionProperties.getLeaseSeconds();
        distributedJobService
                .leaseNext(workerId, leaseSeconds)
                .ifPresent(lease -> distributedJobLeaseRunner.runLease(workerId, leaseSeconds, lease));
    }
}
