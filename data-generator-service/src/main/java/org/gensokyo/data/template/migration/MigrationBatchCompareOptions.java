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
 * Options for batch dual-run compare over the migration inventory catalog.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationBatchCompareOptions implements Serializable {

    /** Merge new {@code db-*} rows from the database before comparing. */
    private boolean refreshInventoryFirst = true;

    /** Skip inventory rows already classified as {@link MigrationClassification#COMPATIBILITY_ONLY}. */
    private boolean skipCompatibilityOnly = true;

    /** Maximum number of templates to compare in one batch (safety cap). */
    private int maxTemplates = 50;

    /** Per-template dual-run options (sample size, key columns). */
    private MigrationCompareOptions compareOptions;

    /**
     * Default batch options for operator-triggered runs.
     *
     * @return batch options
     */
    public static MigrationBatchCompareOptions defaults() {
        return new MigrationBatchCompareOptions();
    }
}
