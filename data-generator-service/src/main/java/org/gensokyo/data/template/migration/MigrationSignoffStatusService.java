/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds per-scenario-family business sign-off status for P3 retirement gates.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class MigrationSignoffStatusService {

    /**
     * Summarizes sign-off progress grouped by {@link MigrationInventoryEntry#getScenarioFamily()}.
     *
     * @param entries inventory rows
     * @return family rollups in stable insertion order
     */
    public List<MigrationSignoffFamilyStatus> summarizeByFamily(List<MigrationInventoryEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        Map<String, MigrationSignoffFamilyStatus> families = new LinkedHashMap<>();

        for (MigrationInventoryEntry entry : entries) {
            String family = entry.getScenarioFamily() != null ? entry.getScenarioFamily() : "unknown";
            MigrationSignoffFamilyStatus status = families.computeIfAbsent(family, key -> {
                MigrationSignoffFamilyStatus created = new MigrationSignoffFamilyStatus();
                created.setScenarioFamily(key);
                return created;
            });
            status.setTotalTemplates(status.getTotalTemplates() + 1);
            if (entry.isBusinessSignoffApproved()) {
                status.setBusinessApproved(status.getBusinessApproved() + 1);
            }
            if (MigrationInventoryBacklogService.isReadyToPromote(entry)
                    && hasCompareReport(entry)) {
                status.setReadyToPromote(status.getReadyToPromote() + 1);
                if (!entry.isBusinessSignoffApproved()) {
                    status.setPendingSignoff(status.getPendingSignoff() + 1);
                }
            }
        }

        for (MigrationSignoffFamilyStatus status : families.values()) {
            // Family gate: every ready row has business sign-off (compatibility-only rows excluded).
            status.setFamilySignoffComplete(status.getPendingSignoff() == 0);
        }
        return new ArrayList<>(families.values());
    }

    private static boolean hasCompareReport(MigrationInventoryEntry entry) {
        return entry.getLastCompareReportPath() != null && !entry.getLastCompareReportPath().isBlank();
    }
}
