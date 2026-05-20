/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary of a batch dual-run compare over the migration inventory.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationBatchCompareResult implements Serializable {

    private int comparedCount;
    private int skippedCount;
    private int failedCount;
    private MigrationInventoryRefreshResult inventoryRefresh;
    private List<MigrationBatchCompareItemResult> items = new ArrayList<>();

    /**
     * Increments the success counter.
     */
    public void recordSuccess() {
        comparedCount++;
    }

    /**
     * Increments the skipped counter.
     */
    public void recordSkipped() {
        skippedCount++;
    }

    /**
     * Increments the failed counter.
     */
    public void recordFailed() {
        failedCount++;
    }
}
