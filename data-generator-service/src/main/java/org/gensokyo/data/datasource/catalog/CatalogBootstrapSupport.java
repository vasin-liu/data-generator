/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ElasticsearchCatalogMetadata;
import org.gensokyo.data.datasource.api.JdbcCatalogMetadata;
import org.gensokyo.data.datasource.api.KafkaCatalogMetadata;
import org.gensokyo.data.elasticsearch.config.MultipleElasticsearchClusterProperties;
import org.gensokyo.data.kafka.config.MultipleKafkaClusterProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registers YAML bootstrap connections into {@link CatalogBootstrapRegistry} at startup (D-25).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
@Component
public class CatalogBootstrapSupport {

    private final CatalogBootstrapRegistry bootstrapRegistry;
    private final ObjectProvider<DynamicDataSourceProperties> dynamicDataSourcePropertiesProvider;
    private final ObjectProvider<MultipleKafkaClusterProperties> kafkaPropertiesProvider;
    private final ObjectProvider<MultipleElasticsearchClusterProperties> elasticsearchPropertiesProvider;

    /**
     * @param bootstrapRegistry                    bootstrap entry store
     * @param dynamicDataSourcePropertiesProvider  YAML JDBC datasource definitions
     * @param kafkaPropertiesProvider              YAML Kafka cluster definitions
     * @param elasticsearchPropertiesProvider      YAML Elasticsearch cluster definitions
     */
    public CatalogBootstrapSupport(
            CatalogBootstrapRegistry bootstrapRegistry,
            ObjectProvider<DynamicDataSourceProperties> dynamicDataSourcePropertiesProvider,
            ObjectProvider<MultipleKafkaClusterProperties> kafkaPropertiesProvider,
            ObjectProvider<MultipleElasticsearchClusterProperties> elasticsearchPropertiesProvider) {
        this.bootstrapRegistry = bootstrapRegistry;
        this.dynamicDataSourcePropertiesProvider = dynamicDataSourcePropertiesProvider;
        this.kafkaPropertiesProvider = kafkaPropertiesProvider;
        this.elasticsearchPropertiesProvider = elasticsearchPropertiesProvider;
    }

    /**
     * Registers bootstrap catalog entries during container startup before managed rows load (D-25).
     */
    @PostConstruct
    void registerAtStartup() {
        registerBootstrapEntries();
    }

    /**
     * Loads JDBC, Kafka, and Elasticsearch bootstrap entries from application configuration.
     */
    public void registerBootstrapEntries() {
        registerJdbcBootstrapEntries();
        registerKafkaBootstrapEntries();
        registerElasticsearchBootstrapEntries();
    }

    private void registerJdbcBootstrapEntries() {
        DynamicDataSourceProperties properties = dynamicDataSourcePropertiesProvider.getIfAvailable();
        if (properties == null || properties.getDatasource() == null) {
            return;
        }
        for (Map.Entry<String, DataSourceProperty> entry : properties.getDatasource().entrySet()) {
            DataSourceProperty source = entry.getValue();
            if (source == null || !StringUtils.hasText(entry.getKey())) {
                continue;
            }
            bootstrapRegistry.register(new CatalogEntry(
                    entry.getKey(),
                    ConnectionKind.JDBC,
                    CatalogEntrySource.BOOTSTRAP,
                    new JdbcCatalogMetadata(source.getUrl(), source.getDriverClassName())));
        }
    }

    private void registerKafkaBootstrapEntries() {
        MultipleKafkaClusterProperties properties = kafkaPropertiesProvider.getIfAvailable();
        if (properties == null || properties.getClusters() == null) {
            return;
        }
        properties.getClusters().forEach((name, cluster) -> {
            if (!StringUtils.hasText(name) || cluster == null) {
                return;
            }
            String servers = cluster.getBootstrapServers() == null
                    ? ""
                    : String.join(",", cluster.getBootstrapServers());
            bootstrapRegistry.register(new CatalogEntry(
                    name,
                    ConnectionKind.KAFKA,
                    CatalogEntrySource.BOOTSTRAP,
                    new KafkaCatalogMetadata(servers)));
        });
    }

    private void registerElasticsearchBootstrapEntries() {
        MultipleElasticsearchClusterProperties properties = elasticsearchPropertiesProvider.getIfAvailable();
        if (properties == null || properties.getClusters() == null) {
            return;
        }
        properties.getClusters().forEach((name, cluster) -> {
            if (!StringUtils.hasText(name) || cluster == null) {
                return;
            }
            String hosts = cluster.getUris() == null
                    ? ""
                    : cluster.getUris().stream().collect(Collectors.joining(","));
            bootstrapRegistry.register(new CatalogEntry(
                    name,
                    ConnectionKind.ELASTICSEARCH,
                    CatalogEntrySource.BOOTSTRAP,
                    new ElasticsearchCatalogMetadata(hosts)));
        });
    }
}
