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
 * Unit tests for {@link MigrationInventoryBacklogService}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class MigrationInventoryBacklogServiceTests {

    private final MigrationInventoryBacklogService service = new MigrationInventoryBacklogService();

    @Test
    void pendingSignoffReturnsReadyRowsWithoutBusinessApproval() {
        MigrationInventoryEntry ready = readyEntry("ready-1", false);
        MigrationInventoryEntry signed = readyEntry("ready-2", true);
        MigrationInventoryEntry blocked = new MigrationInventoryEntry();
        blocked.setId("blocked-1");
        blocked.setMigrationClass(MigrationClassification.BLOCKED);
        blocked.setLastCompareReportPath("docs/migration/reports/x.md");

        List<MigrationInventoryEntry> pending = service.filter(
                List.of(ready, signed, blocked), MigrationBacklogFilter.PENDING_SIGNOFF);

        Assertions.assertEquals(1, pending.size());
        Assertions.assertEquals("ready-1", pending.getFirst().getId());
    }

    private static MigrationInventoryEntry readyEntry(String id, boolean approved) {
        MigrationInventoryEntry entry = new MigrationInventoryEntry();
        entry.setId(id);
        entry.setMigrationClass(MigrationClassification.EXACT);
        entry.setLastCompareReportPath("docs/migration/reports/" + id + ".md");
        entry.setBusinessSignoffApproved(approved);
        return entry;
    }
}
