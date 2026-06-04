/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.datasource.DataSourceConfigSummary;
import org.gensokyo.data.datasource.BundledJdbcDriverRegistry;
import org.gensokyo.data.datasource.JdbcDriverPresetCatalog;

import java.util.List;

/**
 * Persisted JDBC configs plus runtime registry keys for the datasources page.
 *
 * @param persisted      rows from {@code datasource_config}
 * @param runtimeKeys    merged yaml + persisted JDBC keys
 * @param driverPresets  built-in JDBC driver catalog for the console form
 * @param kafkaClusters  Kafka cluster ids from application config (read-only)
 * @param elasticsearchClusters Elasticsearch cluster ids from application config (read-only)
 */
public record DataSourcesOverviewDto(
        List<DataSourceConfigSummary> persisted,
        List<String> runtimeKeys,
        List<JdbcDriverPresetDto> driverPresets,
        List<String> kafkaClusters,
        List<String> elasticsearchClusters) {

    /**
     * @param persisted   persisted rows
     * @param runtimeKeys runtime keys
     * @return overview with catalog presets
     */
    public static DataSourcesOverviewDto of(
            List<DataSourceConfigSummary> persisted,
            List<String> runtimeKeys,
            BundledJdbcDriverRegistry bundledDrivers,
            List<String> kafkaClusters,
            List<String> elasticsearchClusters) {
        List<JdbcDriverPresetDto> presets = JdbcDriverPresetCatalog.all().stream()
                .map(p -> JdbcDriverPresetDto.from(p, bundledDrivers))
                .toList();
        return new DataSourcesOverviewDto(
                persisted, runtimeKeys, presets, kafkaClusters, elasticsearchClusters);
    }
}
