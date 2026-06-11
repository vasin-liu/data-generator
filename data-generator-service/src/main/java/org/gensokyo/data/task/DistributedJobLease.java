/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import java.io.Serializable;
import java.time.Instant;

/**
 * Lease view returned to workers when claiming queue rows.
 *
 * @param jobId      queue row id
 * @param instanceId template run instance id
 * @param templateId template id
 * @param leaseUntil lease expiry timestamp
 * @param payloadJson optional opaque execution payload
 * @author Gensokyo
 * @since 2026-06-01
 */
public record DistributedJobLease(
        Long jobId,
        Long instanceId,
        Long templateId,
        Instant leaseUntil,
        String payloadJson) implements Serializable {
}

