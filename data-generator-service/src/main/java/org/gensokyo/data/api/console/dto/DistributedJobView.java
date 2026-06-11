/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;

/**
 * Read-only distributed queue row linked to a task execution instance.
 *
 * @param jobId       queue row id
 * @param status      queue status
 * @param workerId    worker holding the lease when active
 * @param leaseUntil  lease expiry when leased
 * @param attempts    lease attempt count
 * @param queuedAt    enqueue timestamp
 * @param finishedAt  terminal timestamp when finished
 * @author Gensokyo
 * @since 2026-06-01
 */
public record DistributedJobView(
        @JsonSerialize(using = ToStringSerializer.class) Long jobId,
        String status,
        String workerId,
        Instant leaseUntil,
        Integer attempts,
        Instant queuedAt,
        Instant finishedAt) {
}
