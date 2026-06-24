/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.support;

import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.CatalogMetadata;
import org.gensokyo.data.datasource.api.CatalogResolveSupport;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ElasticsearchCatalogMetadata;
import org.gensokyo.data.datasource.api.ElasticsearchResolvedConnection;
import org.gensokyo.data.datasource.api.JdbcCatalogMetadata;
import org.gensokyo.data.datasource.api.JdbcResolvedConnection;
import org.gensokyo.data.datasource.api.KafkaCatalogMetadata;
import org.gensokyo.data.datasource.api.KafkaResolvedConnection;
import org.gensokyo.data.datasource.api.ResolvedConnection;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test {@link ConnectionCatalog} registering H2/Kafka/ES handles without a full Spring context (D-31).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public final class InMemoryCatalog implements ConnectionCatalog {

    private final Map<String, CatalogEntry> entries = new LinkedHashMap<>();
    private final Map<String, ResolvedConnection> resolved = new LinkedHashMap<>();

    /**
     * Registers a JDBC connection for tests.
     *
     * @param name       connection name
     * @param dataSource JDBC handle
     * @param source     bootstrap or managed tag
     * @param metadata   list metadata
     * @return this catalog for chaining
     */
    public InMemoryCatalog putJdbc(
            String name,
            DataSource dataSource,
            CatalogEntrySource source,
            JdbcCatalogMetadata metadata) {
        entries.put(key(name, ConnectionKind.JDBC), new CatalogEntry(name, ConnectionKind.JDBC, source, metadata));
        resolved.put(key(name, ConnectionKind.JDBC), new JdbcResolvedConnection(name, dataSource));
        return this;
    }

    /**
     * Registers a Kafka producer handle for tests.
     *
     * @param name            cluster name
     * @param producerHandle  opaque Kafka template
     * @param source          bootstrap or managed tag
     * @param metadata        list metadata
     * @return this catalog for chaining
     */
    public InMemoryCatalog putKafka(
            String name,
            Object producerHandle,
            CatalogEntrySource source,
            KafkaCatalogMetadata metadata) {
        entries.put(key(name, ConnectionKind.KAFKA), new CatalogEntry(name, ConnectionKind.KAFKA, source, metadata));
        resolved.put(key(name, ConnectionKind.KAFKA), new KafkaResolvedConnection(name, producerHandle));
        return this;
    }

    /**
     * Registers an Elasticsearch client handle for tests.
     *
     * @param name         cluster name
     * @param clientHandle opaque REST client
     * @param source       bootstrap or managed tag
     * @param metadata     list metadata
     * @return this catalog for chaining
     */
    public InMemoryCatalog putElasticsearch(
            String name,
            Object clientHandle,
            CatalogEntrySource source,
            ElasticsearchCatalogMetadata metadata) {
        entries.put(key(name, ConnectionKind.ELASTICSEARCH),
                new CatalogEntry(name, ConnectionKind.ELASTICSEARCH, source, metadata));
        resolved.put(key(name, ConnectionKind.ELASTICSEARCH), new ElasticsearchResolvedConnection(name, clientHandle));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResolvedConnection resolve(String name, ConnectionKind kind) {
        ResolvedConnection connection = resolved.get(key(name, kind));
        if (connection == null) {
            throw CatalogResolveSupport.unknownConnection(name, kind, "Register the connection in InMemoryCatalog");
        }
        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CatalogEntry> listAll() {
        return List.copyOf(entries.values());
    }

    /**
     * @return true when at least one Kafka entry is registered
     */
    public boolean hasKafka() {
        return entries.values().stream().anyMatch(entry -> entry.kind() == ConnectionKind.KAFKA);
    }

    /**
     * @return true when at least one Elasticsearch entry is registered
     */
    public boolean hasElasticsearch() {
        return entries.values().stream().anyMatch(entry -> entry.kind() == ConnectionKind.ELASTICSEARCH);
    }

    private static String key(String name, ConnectionKind kind) {
        return name + "#" + kind.name();
    }

    /**
     * Convenience builder for calcite tests that only need Kafka.
     *
     * @param cluster        cluster name
     * @param producerHandle Kafka template
     * @return catalog with one Kafka entry
     */
    public static InMemoryCatalog kafkaOnly(String cluster, Object producerHandle) {
        return new InMemoryCatalog().putKafka(
                cluster,
                producerHandle,
                CatalogEntrySource.BOOTSTRAP,
                new KafkaCatalogMetadata("embedded"));
    }

    /**
     * Convenience builder for calcite tests that only need Elasticsearch.
     *
     * @param cluster      cluster name
     * @param clientHandle REST client
     * @return catalog with one Elasticsearch entry
     */
    public static InMemoryCatalog elasticsearchOnly(String cluster, Object clientHandle) {
        return new InMemoryCatalog().putElasticsearch(
                cluster,
                clientHandle,
                CatalogEntrySource.BOOTSTRAP,
                new ElasticsearchCatalogMetadata("embedded"));
    }
}
