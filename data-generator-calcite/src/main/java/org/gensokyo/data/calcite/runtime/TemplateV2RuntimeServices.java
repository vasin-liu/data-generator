/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.elasticsearch.client.RestClient;
import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ElasticsearchResolvedConnection;
import org.gensokyo.data.datasource.api.KafkaResolvedConnection;
import org.gensokyo.data.datasource.api.ResolvedConnection;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Runtime service handles exposed to Template V2 plugins via {@link TemplateV2RuntimeContext} (D-28).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public record TemplateV2RuntimeServices(
        NamedParameterJdbcTemplate jdbcTemplate,
        ConnectionCatalog connectionCatalog,
        AiRuntimeBridge aiRuntimeBridge) {

    /**
     * @param jdbcTemplate      primary JDBC template
     * @param connectionCatalog unified connection catalog
     */
    public TemplateV2RuntimeServices(NamedParameterJdbcTemplate jdbcTemplate, ConnectionCatalog connectionCatalog) {
        this(jdbcTemplate, connectionCatalog, null);
    }

    /**
     * Resolves a Kafka producer template for the given cluster name.
     *
     * @param cluster catalog cluster name
     * @return Kafka template handle
     */
    public KafkaTemplate<String, String> kafkaTemplate(String cluster) {
        if (connectionCatalog == null) {
            throw new IllegalStateException("Connection catalog is not configured");
        }
        ResolvedConnection resolved = connectionCatalog.resolve(cluster, ConnectionKind.KAFKA);
        if (!(resolved instanceof KafkaResolvedConnection kafka)) {
            throw new IllegalStateException("Expected Kafka connection for cluster: " + cluster);
        }
        return (KafkaTemplate<String, String>) kafka.producerHandle();
    }

    /**
     * Resolves an Elasticsearch low-level REST client for the given cluster name.
     *
     * @param cluster catalog cluster name
     * @return Elasticsearch REST client
     */
    public RestClient elasticsearchClient(String cluster) {
        if (connectionCatalog == null) {
            throw new IllegalStateException("Connection catalog is not configured");
        }
        ResolvedConnection resolved = connectionCatalog.resolve(cluster, ConnectionKind.ELASTICSEARCH);
        if (!(resolved instanceof ElasticsearchResolvedConnection elasticsearch)) {
            throw new IllegalStateException("Expected Elasticsearch connection for cluster: " + cluster);
        }
        return (RestClient) elasticsearch.clientHandle();
    }

    /**
     * @return true when the catalog exposes at least one Kafka entry
     */
    public boolean hasKafka() {
        if (connectionCatalog == null) {
            return false;
        }
        return connectionCatalog.listAll().stream()
                .anyMatch(entry -> entry.kind() == ConnectionKind.KAFKA);
    }

    /**
     * @return true when the catalog exposes at least one Elasticsearch entry
     */
    public boolean hasElasticsearch() {
        if (connectionCatalog == null) {
            return false;
        }
        return connectionCatalog.listAll().stream()
                .anyMatch(entry -> entry.kind() == ConnectionKind.ELASTICSEARCH);
    }
}
