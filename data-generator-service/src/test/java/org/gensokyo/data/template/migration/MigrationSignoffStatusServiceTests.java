/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for {@link MigrationSignoffStatusService}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class MigrationSignoffStatusServiceTests {

    private final MigrationSignoffStatusService service = new MigrationSignoffStatusService();

    @Test
    void familyCompleteWhenNoPendingSignoff() {
        MigrationInventoryEntry approved = new MigrationInventoryEntry();
        approved.setScenarioFamily("synthetic");
        approved.setMigrationClass(MigrationClassification.EXACT);
        approved.setLastCompareReportPath("docs/migration/reports/a.md");
        approved.setBusinessSignoffApproved(true);

        MigrationInventoryEntry compat = new MigrationInventoryEntry();
        compat.setScenarioFamily("synthetic");
        compat.setMigrationClass(MigrationClassification.COMPATIBILITY_ONLY);

        List<MigrationSignoffFamilyStatus> families =
                service.summarizeByFamily(List.of(approved, compat));

        MigrationSignoffFamilyStatus synthetic = families.stream()
                .filter(f -> "synthetic".equals(f.getScenarioFamily()))
                .findFirst()
                .orElseThrow();
        Assertions.assertTrue(synthetic.isFamilySignoffComplete());
        Assertions.assertEquals(1, synthetic.getBusinessApproved());
        Assertions.assertEquals(1, synthetic.getReadyToPromote());
        Assertions.assertEquals(0, synthetic.getPendingSignoff());
    }
}
