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

/**
 * Outcome of merging database V1 templates into the migration scenario inventory.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationInventoryRefreshResult implements Serializable {

    private int addedCount;
    private int totalCount;
    private String inventoryPath;
    private boolean persisted;

    /**
     * Builds a refresh summary.
     *
     * @param addedCount    number of new {@code db-*} rows merged
     * @param totalCount    inventory size after merge
     * @param inventoryPath path to {@code scenario-inventory.yaml}
     * @param persisted     whether the YAML file was rewritten
     * @return refresh result
     */
    public static MigrationInventoryRefreshResult of(
            int addedCount, int totalCount, String inventoryPath, boolean persisted) {
        MigrationInventoryRefreshResult result = new MigrationInventoryRefreshResult();
        result.setAddedCount(addedCount);
        result.setTotalCount(totalCount);
        result.setInventoryPath(inventoryPath);
        result.setPersisted(persisted);
        return result;
    }
}
