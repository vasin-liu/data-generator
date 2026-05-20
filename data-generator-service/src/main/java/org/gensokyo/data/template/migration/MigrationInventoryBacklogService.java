/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Filters migration inventory rows for operator work queues.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class MigrationInventoryBacklogService {

    /**
     * Returns inventory entries matching the backlog filter.
     *
     * @param entries all inventory rows
     * @param filter  backlog filter; when {@code null}, {@link MigrationBacklogFilter#ALL} is used
     * @return matching entries (defensive copy)
     */
    public List<MigrationInventoryEntry> filter(List<MigrationInventoryEntry> entries, MigrationBacklogFilter filter) {
        Objects.requireNonNull(entries, "entries");
        MigrationBacklogFilter effective = filter != null ? filter : MigrationBacklogFilter.ALL;
        List<MigrationInventoryEntry> result = new ArrayList<>();
        for (MigrationInventoryEntry entry : entries) {
            if (matches(entry, effective)) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Parses a filter name from a query parameter (case-insensitive).
     *
     * @param filterName filter name or {@code null}
     * @return parsed filter, or {@link MigrationBacklogFilter#ALL} when blank
     * @throws IllegalArgumentException when the name is unknown
     */
    public static MigrationBacklogFilter parseFilter(String filterName) {
        if (filterName == null || filterName.isBlank()) {
            return MigrationBacklogFilter.ALL;
        }
        try {
            return MigrationBacklogFilter.valueOf(filterName.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown backlog filter: " + filterName, ex);
        }
    }

    private static boolean matches(MigrationInventoryEntry entry, MigrationBacklogFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case READY -> isReadyToPromote(entry)
                    && hasCompareReport(entry);
            case BLOCKED -> entry.getMigrationClass() == MigrationClassification.BLOCKED;
            case COMPATIBILITY_ONLY -> entry.getMigrationClass() == MigrationClassification.COMPATIBILITY_ONLY;
            case NEEDS_COMPARE -> entry.getLastCompareReportPath() == null
                    || entry.getLastCompareReportPath().isBlank();
            case PENDING_SIGNOFF -> isReadyToPromote(entry)
                    && hasCompareReport(entry)
                    && !entry.isBusinessSignoffApproved();
        };
    }

    private static boolean hasCompareReport(MigrationInventoryEntry entry) {
        return entry.getLastCompareReportPath() != null && !entry.getLastCompareReportPath().isBlank();
    }

    static boolean isReadyToPromote(MigrationInventoryEntry entry) {
        MigrationClassification classification = entry.getMigrationClass();
        if (classification == null) {
            return false;
        }
        return classification == MigrationClassification.EXACT
                || classification == MigrationClassification.ADAPTED
                || classification == MigrationClassification.APPROXIMATE;
    }
}
