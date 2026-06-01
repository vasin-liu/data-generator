/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Template execution policy overrides for scale, preview, and partitioned compute.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Getter
@Setter
public class ExecutionPolicyVO implements Serializable {
    private String mode;
    private Integer maxRowsInMemory;
    /** Optional cap on total rows processed in a run; enforced when set. */
    private Integer maxTotalRows;
    private Integer previewRowLimit;
    private Integer sourceChunkSize;
    private Integer sinkBatchSize;
    private Boolean failOnLimitExceeded;
    private Integer broadcastMaxRows;
    /** In-process partition count for compute blocks; {@code 1} or unset disables partitioning. */
    private Integer partitionCount;
    /** Optional column name used to hash rows into partitions; round-robin when unset. */
    private String partitionKey;
}
