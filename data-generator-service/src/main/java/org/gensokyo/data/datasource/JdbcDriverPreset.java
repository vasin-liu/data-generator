/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import java.util.List;

/**
 * Built-in JDBC driver preset for console datasource forms (primary + alternate driver classes).
 *
 * @param id                        stable preset id
 * @param groupKey                  UI grouping key (dm, kingbase, …)
 * @param bundleKey                 shipped JAR directory under {@code jdbc-bundled/}
 * @param labelKey                    i18n key for display label
 * @param primaryDriverClassName      preferred driver class
 * @param alternateDriverClassNames   fallbacks when JAR uses another entry point
 * @param urlTemplate                 suggested JDBC URL
 * @author Gensokyo
 * @since 2026-05-29
 */
public record JdbcDriverPreset(
        String id,
        String groupKey,
        String bundleKey,
        String labelKey,
        String primaryDriverClassName,
        List<String> alternateDriverClassNames,
        String urlTemplate) {

    /**
     * @return ordered driver class names to try (primary first, then alternates)
     */
    public List<String> driverClassCandidates() {
        return JdbcDriverPresetCatalog.mergeCandidates(primaryDriverClassName, alternateDriverClassNames);
    }
}
