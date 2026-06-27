/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.CatalogMetadata;
import org.gensokyo.data.datasource.api.CatalogResolveSupport;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionHealthStatus;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ConnectionTestRequest;
import org.gensokyo.data.datasource.api.ConnectionTestResult;
import org.gensokyo.data.datasource.api.ElasticsearchCatalogMetadata;
import org.gensokyo.data.datasource.api.ElasticsearchResolvedConnection;
import org.gensokyo.data.datasource.api.JdbcCatalogMetadata;
import org.gensokyo.data.datasource.api.JdbcResolvedConnection;
import org.gensokyo.data.datasource.api.KafkaCatalogMetadata;
import org.gensokyo.data.datasource.api.KafkaResolvedConnection;
import org.gensokyo.data.datasource.api.ResolvedConnection;
import org.gensokyo.data.datasource.elasticsearch.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.datasource.kafka.DynamicKafkaTemplateRegistry;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.messaging.MessagingClusterConfigService.ElasticsearchClusterConfig;
import org.gensokyo.data.messaging.MessagingClusterConfigService.KafkaClusterConfig;
import org.gensokyo.data.messaging.MessagingClusterType;
import org.gensokyo.data.model.po.MessagingClusterConfigPO;
import org.gensokyo.data.repository.MessagingClusterConfigRepository;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Production {@link ConnectionCatalog} merging yaml bootstrap and DB-managed entries (D-05, D-06, D-23).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
@Service
@RequiredArgsConstructor
public class ConnectionCatalogImpl implements ConnectionCatalog {

    private final ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider;
    private final ObjectProvider<DynamicKafkaTemplateRegistry> kafkaRegistryProvider;
    private final ObjectProvider<DynamicElasticsearchClientRegistry> elasticsearchRegistryProvider;
    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final MessagingClusterConfigRepository messagingClusterConfigRepository;
    private final HotReloadCoordinator hotReloadCoordinator;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResolvedConnection resolve(String name, ConnectionKind kind) {
        if (StrKit.isBlank(name)) {
            throw CatalogResolveSupport.unknownConnection(name, kind, "Connection name must not be blank");
        }
        return switch (kind) {
            case JDBC -> resolveJdbc(name.trim());
            case KAFKA -> resolveKafka(name.trim());
            case ELASTICSEARCH -> resolveElasticsearch(name.trim());
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CatalogEntry> listAll() {
        Map<String, CatalogEntry> merged = new LinkedHashMap<>();
        appendJdbcEntries(merged);
        appendKafkaEntries(merged);
        appendElasticsearchEntries(merged);
        return List.copyOf(merged.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionTestResult test(ConnectionTestRequest request) {
        // Wave 2 delegates to JDBC/Kafka/ES adapter connectivity checks (07-02).
        throw new UnsupportedOperationException("ConnectionCatalog.test is implemented in Phase 7 Wave 2");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CatalogEntry reload(String name, ConnectionKind kind) {
        if (StrKit.isBlank(name)) {
            throw new IllegalArgumentException("Connection name must not be blank");
        }
        String trimmed = name.trim();
        CatalogEntry base = findEntry(trimmed, kind)
                .orElseThrow(() -> CatalogResolveSupport.unknownConnection(
                        trimmed, kind, "Cannot reload unknown connection"));
        return hotReloadCoordinator.reload(trimmed, kind, base);
    }

    public boolean isBootstrapOnly(String name) {
        return isBootstrapOnlyJdbcName(name);
    }

    /**
     * Returns true when the name is registered only from yaml bootstrap (not persisted MANAGED JDBC).
     *
     * @param name connection name
     * @return true when the JDBC entry is bootstrap-only
     */
    public boolean isBootstrapOnlyJdbcName(String name) {
        if (StrKit.isBlank(name)) {
            return false;
        }
        Set<String> managed = managedJdbcNames();
        return listRuntimeJdbcNames().contains(name) && !managed.contains(name);
    }

    private ResolvedConnection resolveJdbc(String name) {
        DynamicRoutingDataSource routing = dynamicRoutingDataSourceProvider.getIfAvailable();
        if (routing == null) {
            throw CatalogResolveSupport.unknownConnection(
                    name, ConnectionKind.JDBC, "DynamicRoutingDataSource is not available");
        }
        DataSource dataSource = routing.getDataSources().get(name);
        if (dataSource == null) {
            throw CatalogResolveSupport.unknownConnection(
                    name, ConnectionKind.JDBC, "Check console datasource list or application yaml bootstrap entries");
        }
        return new JdbcResolvedConnection(name, dataSource);
    }

    private ResolvedConnection resolveKafka(String name) {
        DynamicKafkaTemplateRegistry registry = kafkaRegistryProvider.getIfAvailable();
        if (registry == null) {
            throw CatalogResolveSupport.unknownConnection(
                    name, ConnectionKind.KAFKA, "Kafka cluster registry is not available");
        }
        try {
            KafkaTemplate<String, String> template = registry.template(name);
            return new KafkaResolvedConnection(name, template);
        } catch (IllegalArgumentException ex) {
            throw CatalogResolveSupport.unknownConnection(
                    name, ConnectionKind.KAFKA, "Check console Kafka cluster list or application yaml bootstrap entries");
        }
    }

    private ResolvedConnection resolveElasticsearch(String name) {
        DynamicElasticsearchClientRegistry registry = elasticsearchRegistryProvider.getIfAvailable();
        if (registry == null) {
            throw CatalogResolveSupport.unknownConnection(
                    name, ConnectionKind.ELASTICSEARCH, "Elasticsearch cluster registry is not available");
        }
        try {
            return new ElasticsearchResolvedConnection(name, registry.llc(name));
        } catch (IllegalArgumentException ex) {
            throw CatalogResolveSupport.unknownConnection(
                    name, ConnectionKind.ELASTICSEARCH,
                    "Check console Elasticsearch cluster list or application yaml bootstrap entries");
        }
    }

    private void appendJdbcEntries(Map<String, CatalogEntry> merged) {
        Set<String> managedNames = managedJdbcNames();
        for (String runtimeName : listRuntimeJdbcNames()) {
            CatalogEntrySource source = managedNames.contains(runtimeName)
                    ? CatalogEntrySource.MANAGED
                    : CatalogEntrySource.BOOTSTRAP;
            merged.put(runtimeName, entry(
                    runtimeName,
                    ConnectionKind.JDBC,
                    source,
                    jdbcMetadata(runtimeName, managedNames),
                    null));
        }
        for (DataSourceConfigPO row : dataSourceConfigRepository.findByEnabledTrue()) {
            if (!merged.containsKey(row.getName())) {
                merged.put(row.getName(), entry(
                        row.getName(),
                        ConnectionKind.JDBC,
                        CatalogEntrySource.MANAGED,
                        new JdbcCatalogMetadata(row.getUrl(), row.getDriverClassName()),
                        row.getUpdatedAt()));
            } else {
                merged.put(row.getName(), entry(
                        row.getName(),
                        ConnectionKind.JDBC,
                        CatalogEntrySource.MANAGED,
                        new JdbcCatalogMetadata(row.getUrl(), row.getDriverClassName()),
                        row.getUpdatedAt()));
            }
        }
    }

    private void appendKafkaEntries(Map<String, CatalogEntry> merged) {
        DynamicKafkaTemplateRegistry registry = kafkaRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        Set<String> managedNames = managedMessagingNames(MessagingClusterType.KAFKA);
        for (String cluster : registry.getTemplates().keySet()) {
            putMessagingEntry(merged, cluster, ConnectionKind.KAFKA, managedNames.contains(cluster),
                    new KafkaCatalogMetadata(cluster));
        }
        for (MessagingClusterConfigPO row : messagingClusterConfigRepository.findByClusterType(MessagingClusterType.KAFKA.name())) {
            if (!Boolean.TRUE.equals(row.getEnabled())) {
                continue;
            }
            KafkaClusterConfig config = readKafkaConfig(row);
            String brokers = config == null || config.bootstrapServers() == null || config.bootstrapServers().isEmpty()
                    ? row.getName()
                    : String.join(",", config.bootstrapServers());
            merged.put(row.getName(), entry(
                    row.getName(),
                    ConnectionKind.KAFKA,
                    CatalogEntrySource.MANAGED,
                    new KafkaCatalogMetadata(brokers),
                    row.getUpdatedAt()));
        }
    }

    private void appendElasticsearchEntries(Map<String, CatalogEntry> merged) {
        DynamicElasticsearchClientRegistry registry = elasticsearchRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        Set<String> managedNames = managedMessagingNames(MessagingClusterType.ELASTICSEARCH);
        for (String cluster : registry.getLowLevelClients().keySet()) {
            putMessagingEntry(merged, cluster, ConnectionKind.ELASTICSEARCH, managedNames.contains(cluster),
                    new ElasticsearchCatalogMetadata(cluster));
        }
        for (MessagingClusterConfigPO row : messagingClusterConfigRepository.findByClusterType(MessagingClusterType.ELASTICSEARCH.name())) {
            if (!Boolean.TRUE.equals(row.getEnabled())) {
                continue;
            }
            ElasticsearchClusterConfig config = readElasticsearchConfig(row);
            String hosts = config == null || config.uris() == null || config.uris().isEmpty()
                    ? row.getName()
                    : String.join(",", config.uris());
            merged.put(row.getName(), entry(
                    row.getName(),
                    ConnectionKind.ELASTICSEARCH,
                    CatalogEntrySource.MANAGED,
                    new ElasticsearchCatalogMetadata(hosts),
                    row.getUpdatedAt()));
        }
    }

    private void putMessagingEntry(
            Map<String, CatalogEntry> merged,
            String name,
            ConnectionKind kind,
            boolean managed,
            CatalogMetadata metadata) {
        CatalogEntrySource source = managed ? CatalogEntrySource.MANAGED : CatalogEntrySource.BOOTSTRAP;
        merged.put(name, entry(name, kind, source, metadata, null));
    }

    private CatalogEntry entry(
            String name,
            ConnectionKind kind,
            CatalogEntrySource source,
            CatalogMetadata metadata,
            Instant updatedAt) {
        long version = updatedAt != null ? updatedAt.toEpochMilli() : 1L;
        CatalogEntry base = new CatalogEntry(
                name,
                kind,
                source,
                metadata,
                version,
                updatedAt,
                ConnectionHealthStatus.HEALTHY,
                null,
                null);
        return hotReloadCoordinator.healthOverlay(name, kind)
                .map(overlay -> hotReloadCoordinator.overlay(base, overlay))
                .orElse(base);
    }

    private CatalogMetadata jdbcMetadata(String runtimeName, Set<String> managedNames) {
        return dataSourceConfigRepository.findById(runtimeName)
                .filter(row -> Boolean.TRUE.equals(row.getEnabled()))
                .map(row -> (CatalogMetadata) new JdbcCatalogMetadata(row.getUrl(), row.getDriverClassName()))
                .orElseGet(() -> bootstrapJdbcMetadata(runtimeName, managedNames));
    }

    private CatalogMetadata bootstrapJdbcMetadata(String runtimeName, Set<String> managedNames) {
        if (managedNames.contains(runtimeName)) {
            return new JdbcCatalogMetadata("", null);
        }
        DynamicRoutingDataSource routing = dynamicRoutingDataSourceProvider.getIfAvailable();
        if (routing == null) {
            return new JdbcCatalogMetadata("", null);
        }
        DataSource dataSource = routing.getDataSources().get(runtimeName);
        if (dataSource instanceof DruidDataSource druid) {
            return new JdbcCatalogMetadata(druid.getUrl(), druid.getDriverClassName());
        }
        return new JdbcCatalogMetadata("", null);
    }

    private Set<String> managedMessagingNames(MessagingClusterType type) {
        return messagingClusterConfigRepository.findByClusterType(type.name()).stream()
                .filter(row -> Boolean.TRUE.equals(row.getEnabled()))
                .map(MessagingClusterConfigPO::getName)
                .collect(Collectors.toSet());
    }

    private KafkaClusterConfig readKafkaConfig(MessagingClusterConfigPO row) {
        if (row.getConfigJson() == null || row.getConfigJson().isBlank()) {
            return null;
        }
        return TemplateJsonCodec.read(row.getConfigJson(), KafkaClusterConfig.class);
    }

    private ElasticsearchClusterConfig readElasticsearchConfig(MessagingClusterConfigPO row) {
        if (row.getConfigJson() == null || row.getConfigJson().isBlank()) {
            return null;
        }
        return TemplateJsonCodec.read(row.getConfigJson(), ElasticsearchClusterConfig.class);
    }

    private Set<String> listRuntimeJdbcNames() {
        DynamicRoutingDataSource routing = dynamicRoutingDataSourceProvider.getIfAvailable();
        if (routing == null) {
            return Set.of();
        }
        return routing.getDataSources().keySet();
    }

    private Set<String> managedJdbcNames() {
        return dataSourceConfigRepository.findByEnabledTrue().stream()
                .map(DataSourceConfigPO::getName)
                .collect(Collectors.toSet());
    }
}
