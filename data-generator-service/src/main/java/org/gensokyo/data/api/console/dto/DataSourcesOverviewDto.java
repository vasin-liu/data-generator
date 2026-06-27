/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.datasource.DataSourceConfigSummary;
import org.gensokyo.data.datasource.BundledJdbcDriverRegistry;
import org.gensokyo.data.datasource.JdbcDriverPresetCatalog;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.messaging.MessagingClusterConfigService;

import java.util.List;

/**
 * Persisted JDBC configs plus runtime registry keys for the datasources page.
 *
 * @param persisted      rows from {@code datasource_config}
 * @param runtimeKeys    merged yaml + persisted JDBC keys
 * @param driverPresets  built-in JDBC driver catalog for the console form
 * @param kafkaClusters  merged Kafka cluster ids (yaml + console-managed)
 * @param elasticsearchClusters merged Elasticsearch cluster ids
 * @param kafkaPersisted console-managed Kafka rows
 * @param elasticsearchPersisted console-managed Elasticsearch rows
 * @param catalogConnections merged catalog entries with BOOTSTRAP/MANAGED source tags
 * @param governance       profile-gated connectivity test flags for the console
 */
public record DataSourcesOverviewDto(
        List<DataSourceConfigSummary> persisted,
        List<String> runtimeKeys,
        List<JdbcDriverPresetDto> driverPresets,
        List<String> kafkaClusters,
        List<String> elasticsearchClusters,
        List<MessagingClusterSummaryDto> kafkaPersisted,
        List<MessagingClusterSummaryDto> elasticsearchPersisted,
        List<CatalogConnectionSummaryDto> catalogConnections,
        DatasourceGovernanceFlagsDto governance) {

    /**
     * @param persisted   persisted rows
     * @param runtimeKeys runtime keys
     * @return overview with catalog presets
     */
    public static DataSourcesOverviewDto of(
            List<DataSourceConfigSummary> persisted,
            List<String> runtimeKeys,
            BundledJdbcDriverRegistry bundledDrivers,
            MessagingClusterConfigService messagingClusterConfigService,
            ConnectionCatalog connectionCatalog,
            DataGeneratorProperties properties) {
        List<JdbcDriverPresetDto> presets = JdbcDriverPresetCatalog.all().stream()
                .map(p -> JdbcDriverPresetDto.from(p, bundledDrivers))
                .toList();
        List<CatalogConnectionSummaryDto> catalogConnections = connectionCatalog.listAll().stream()
                .map(CatalogConnectionSummaryDto::from)
                .toList();
        return new DataSourcesOverviewDto(
                persisted,
                runtimeKeys,
                presets,
                messagingClusterConfigService.listKafkaClusterKeys(),
                messagingClusterConfigService.listElasticsearchClusterKeys(),
                messagingClusterConfigService.listKafka().stream().map(MessagingClusterSummaryDto::from).toList(),
                messagingClusterConfigService.listElasticsearch().stream()
                        .map(MessagingClusterSummaryDto::from)
                        .toList(),
                catalogConnections,
                governanceFlags(properties));
    }

    private static DatasourceGovernanceFlagsDto governanceFlags(DataGeneratorProperties properties) {
        DataGeneratorProperties.Governance governance = properties.getGovernance();
        return new DatasourceGovernanceFlagsDto(
                governance.isRequireConnectivityTestBeforeSave(),
                governance.isRequireConnectivityTestBeforePublish());
    }
}
