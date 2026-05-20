/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

/**
 * Filters for {@link MigrationInventoryBacklogService}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public enum MigrationBacklogFilter {

    /** All inventory rows. */
    ALL,
    /** EXACT / ADAPTED / APPROXIMATE with a compare report. */
    READY,
    /** BLOCKED classification. */
    BLOCKED,
    /** COMPATIBILITY_ONLY classification. */
    COMPATIBILITY_ONLY,
    /** No {@code lastCompareReportPath} yet. */
    NEEDS_COMPARE,
    /** Ready to promote but business sign-off not recorded. */
    PENDING_SIGNOFF
}
