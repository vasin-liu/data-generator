/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.datasource.BundledJdbcDriverRegistry;
import org.gensokyo.data.datasource.JdbcDriverPreset;

import java.util.List;

/**
 * Console API view of a JDBC driver preset.
 *
 * @param id                        preset id
 * @param groupKey                  UI group
 * @param bundleKey                 shipped JAR bundle id
 * @param labelKey                  i18n label key
 * @param driverClassName           primary driver class
 * @param alternateDriverClassNames version/vendor alternates
 * @param urlTemplate               suggested JDBC URL
 * @param bundled                   true when driver JARs ship with the service
 * @author Gensokyo
 * @since 2026-05-29
 */
public record JdbcDriverPresetDto(
        String id,
        String groupKey,
        String bundleKey,
        String labelKey,
        String driverClassName,
        List<String> alternateDriverClassNames,
        String urlTemplate,
        boolean bundled) {

    /**
     * @param preset   catalog entry
     * @param registry bundled driver registry
     * @return API DTO
     */
    public static JdbcDriverPresetDto from(JdbcDriverPreset preset, BundledJdbcDriverRegistry registry) {
        return new JdbcDriverPresetDto(
                preset.id(),
                preset.groupKey(),
                preset.bundleKey(),
                preset.labelKey(),
                preset.primaryDriverClassName(),
                preset.alternateDriverClassNames(),
                preset.urlTemplate(),
                registry.hasBundle(preset.bundleKey()));
    }
}
