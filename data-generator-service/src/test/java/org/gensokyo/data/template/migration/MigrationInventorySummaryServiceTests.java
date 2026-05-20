/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Unit tests for {@link MigrationInventorySummaryService}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class MigrationInventorySummaryServiceTests {

    private final MigrationInventorySummaryService service = new MigrationInventorySummaryService();

    @Test
    void summarizeCountsClassificationsAndWaves() throws Exception {
        Path inventoryPath = Files.createTempFile("inventory-summary", ".yaml");
        Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
        MigrationInventoryService inventoryService = new MigrationInventoryService(inventoryPath);

        MigrationInventoryEntry exact = new MigrationInventoryEntry();
        exact.setId("regression-exact");
        exact.setOrigin("repository");
        exact.setScenarioFamily("synthetic");
        exact.setMigrationClass(MigrationClassification.EXACT);
        exact.setWave(1);
        exact.setV2DraftPresent(true);
        exact.setLastCompareReportPath("docs/migration/reports/a.md");

        MigrationInventoryEntry blocked = new MigrationInventoryEntry();
        blocked.setId("db-99");
        blocked.setOrigin("database");
        blocked.setDbTemplateId(99L);
        blocked.setScenarioFamily("multi_source");
        blocked.setMigrationClass(MigrationClassification.BLOCKED);
        blocked.setWave(2);

        MigrationInventoryEntry compat = new MigrationInventoryEntry();
        compat.setId("regression-pause");
        compat.setOrigin("repository");
        compat.setMigrationClass(MigrationClassification.COMPATIBILITY_ONLY);
        compat.setWave(1);

        inventoryService.saveAll(List.of(exact, blocked, compat));

        MigrationInventorySummary summary = service.summarize(inventoryService);

        Assertions.assertEquals(3, summary.getTotalTemplates());
        Assertions.assertEquals(1, summary.getDatabaseTemplates());
        Assertions.assertEquals(2, summary.getRepositoryTemplates());
        Assertions.assertEquals(1, summary.getWithCompareReport());
        Assertions.assertEquals(1, summary.getBlocked());
        Assertions.assertEquals(1, summary.getCompatibilityOnly());
        Assertions.assertEquals(1, summary.getReadyToPromote());
        Assertions.assertEquals(1, summary.getByClassification().get("EXACT"));
        Assertions.assertEquals(1, summary.getByClassification().get("BLOCKED"));
        Assertions.assertEquals(2, summary.getByWave().get(1));
        Assertions.assertEquals(1, summary.getByWave().get(2));
    }
}
