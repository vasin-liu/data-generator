/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

/**
 * Lifecycle states for {@code task_execution} rows.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public enum TaskExecutionStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    SUCCESS,
    FAILED,
    CANCELLED
}
