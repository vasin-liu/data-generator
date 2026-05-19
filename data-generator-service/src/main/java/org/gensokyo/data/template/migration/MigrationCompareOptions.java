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
import java.util.List;

/**
 * Options for dual-run migration compare (sample size, key columns, execution hints).
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationCompareOptions implements Serializable {

    private int sampleSize = 500;
    private List<String> keyColumns;
    private boolean preferChunked;

    /**
     * Returns default compare options.
     *
     * @return options with {@code sampleSize = 500}
     */
    public static MigrationCompareOptions defaults() {
        return new MigrationCompareOptions();
    }
}
