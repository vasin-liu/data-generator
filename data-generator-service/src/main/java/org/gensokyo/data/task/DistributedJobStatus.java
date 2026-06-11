/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

/**
 * Queue status for distributed coordinator/worker execution rows.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
public enum DistributedJobStatus {
    QUEUED,
    LEASED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}

