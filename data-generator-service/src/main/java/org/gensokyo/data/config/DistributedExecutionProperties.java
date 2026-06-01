/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Coordinator/worker runtime settings for distributed execution (Phase C2).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = DataGeneratorProperties.PREFIX + ".distributed")
public class DistributedExecutionProperties {

    /**
     * Enables coordinator polling and queue-backed execution.
     */
    private boolean enabled = false;

    /**
     * When {@code true}, {@link org.gensokyo.data.task.DistributedJobWorker} polls and executes queue rows.
     */
    private boolean workerEnabled = false;

    /**
     * When {@code false}, the embedded coordinator does not poll (remote workers only).
     */
    private boolean coordinatorPollEnabled = true;

    /**
     * Fixed-delay polling interval in milliseconds for queue leasing.
     */
    private long pollDelayMs = 2_000L;

    /**
     * Lease ttl in seconds used for claim and heartbeat extension.
     */
    private int leaseSeconds = 30;

    /**
     * Worker identity used by the embedded coordinator/worker process.
     */
    private String workerId = "coordinator-local";
}

