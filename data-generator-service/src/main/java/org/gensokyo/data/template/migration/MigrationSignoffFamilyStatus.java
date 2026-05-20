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
 * Business sign-off rollup for one scenario family in the migration inventory.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
public class MigrationSignoffFamilyStatus implements Serializable {

    private String scenarioFamily;
    private int totalTemplates;
    private int businessApproved;
    private int readyToPromote;
    private int pendingSignoff;
    private boolean familySignoffComplete;
}
