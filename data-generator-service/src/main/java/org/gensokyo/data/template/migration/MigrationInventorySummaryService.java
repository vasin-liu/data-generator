/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds aggregate statistics from the migration scenario inventory.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class MigrationInventorySummaryService {

    /**
     * Summarizes inventory rows for operator review.
     *
     * @param inventoryService inventory persistence
     * @return summary counts and breakdowns
     */
    public MigrationInventorySummary summarize(MigrationInventoryService inventoryService) {
        Objects.requireNonNull(inventoryService, "inventoryService");
        List<MigrationInventoryEntry> entries = inventoryService.listAll();

        MigrationInventorySummary summary = new MigrationInventorySummary();
        summary.setInventoryPath(inventoryService.inventoryPathString());
        summary.setTotalTemplates(entries.size());

        for (MigrationInventoryEntry entry : entries) {
            if ("database".equalsIgnoreCase(entry.getOrigin())) {
                summary.setDatabaseTemplates(summary.getDatabaseTemplates() + 1);
            }
            else {
                summary.setRepositoryTemplates(summary.getRepositoryTemplates() + 1);
            }
            if (entry.isV2DraftPresent()) {
                summary.setWithV2Draft(summary.getWithV2Draft() + 1);
            }
            if (entry.getLastCompareReportPath() != null && !entry.getLastCompareReportPath().isBlank()) {
                summary.setWithCompareReport(summary.getWithCompareReport() + 1);
            }

            MigrationClassification classification =
                    entry.getMigrationClass() != null ? entry.getMigrationClass() : MigrationClassification.UNCLASSIFIED;
            increment(summary.getByClassification(), classification.name());

            if (classification == MigrationClassification.COMPATIBILITY_ONLY) {
                summary.setCompatibilityOnly(summary.getCompatibilityOnly() + 1);
            }
            if (classification == MigrationClassification.BLOCKED) {
                summary.setBlocked(summary.getBlocked() + 1);
            }
            if (isReadyToPromote(classification)
                    && entry.getLastCompareReportPath() != null
                    && !entry.getLastCompareReportPath().isBlank()) {
                summary.setReadyToPromote(summary.getReadyToPromote() + 1);
            }

            if (entry.getScenarioFamily() != null && !entry.getScenarioFamily().isBlank()) {
                increment(summary.getByScenarioFamily(), entry.getScenarioFamily());
            }
            if (entry.getWave() != null) {
                increment(summary.getByWave(), entry.getWave());
            }
        }
        return summary;
    }

    private static boolean isReadyToPromote(MigrationClassification classification) {
        return classification == MigrationClassification.EXACT
                || classification == MigrationClassification.ADAPTED
                || classification == MigrationClassification.APPROXIMATE;
    }

    private static void increment(Map<String, Integer> counts, String key) {
        counts.merge(key, 1, Integer::sum);
    }

    private static void increment(Map<Integer, Integer> counts, Integer key) {
        counts.merge(key, 1, Integer::sum);
    }
}
