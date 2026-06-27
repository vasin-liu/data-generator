/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.JdbcCatalogMetadata;
import org.gensokyo.data.datasource.api.snapshot.ExecutionConnectionSnapshot;
import org.gensokyo.data.datasource.api.snapshot.SnapshottedConnectionRef;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.messaging.MessagingClusterConfigService;
import org.gensokyo.data.messaging.MessagingClusterType;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.model.po.MessagingClusterConfigPO;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.data.repository.MessagingClusterConfigRepository;
import org.gensokyo.data.writer.ElasticsearchWriterVO;
import org.gensokyo.data.writer.KafkaWriterVO;
import org.gensokyo.kit.character.StrKit;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds param-only {@link ExecutionConnectionSnapshot} from Template V2 graphs at RUNNING (D-01..D-08).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@Service
@RequiredArgsConstructor
public class ConnectionSnapshotSupport {

    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final MessagingClusterConfigRepository messagingClusterConfigRepository;

    /**
     * Walks template sources, sinks, and inline blocks to freeze JDBC, Kafka, and ES connection params.
     *
     * @param template active template for the run
     * @param catalog  live catalog for BOOTSTRAP/MANAGED tags and version pinning
     * @return snapshot with param maps only (no runtime handles or plaintext secrets)
     */
    public ExecutionConnectionSnapshot buildSnapshot(TemplateV2VO template, ConnectionCatalog catalog) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(catalog, "catalog");
        Instant capturedAt = Instant.now();
        Set<String> seen = new LinkedHashSet<>();
        List<SnapshottedConnectionRef> connections = new ArrayList<>();
        collectFromSources(template, catalog, capturedAt, seen, connections);
        collectFromSinks(template, catalog, capturedAt, seen, connections);
        return new ExecutionConnectionSnapshot(capturedAt, connections);
    }

    private void collectFromSources(
            TemplateV2VO template,
            ConnectionCatalog catalog,
            Instant capturedAt,
            Set<String> seen,
            List<SnapshottedConnectionRef> connections) {
        if (template.getSources() == null) {
            return;
        }
        for (SourceVO source : template.getSources().values()) {
            if (source instanceof QuerySourceVO query) {
                collectJdbcSource(query.getDataSourceId(), query.getDataSource(), catalog, capturedAt, seen, connections);
            } else if (source instanceof PostGisQuerySourceVO postGis) {
                collectJdbcSource(postGis.getDataSourceId(), postGis.getDataSource(), catalog, capturedAt, seen, connections);
            }
        }
    }

    private void collectFromSinks(
            TemplateV2VO template,
            ConnectionCatalog catalog,
            Instant capturedAt,
            Set<String> seen,
            List<SnapshottedConnectionRef> connections) {
        if (template.getSinks() == null) {
            return;
        }
        for (WriteStageVO stage : template.getSinks()) {
            if (stage == null || stage.getWriters() == null) {
                continue;
            }
            for (WriterVO sink : stage.getWriters()) {
                collectWriterRef(sink, catalog, capturedAt, seen, connections);
            }
        }
    }

    private void collectWriterRef(
            WriterVO sink,
            ConnectionCatalog catalog,
            Instant capturedAt,
            Set<String> seen,
            List<SnapshottedConnectionRef> connections) {
        if (sink == null || sink.getType() == null) {
            return;
        }
        String type = sink.getType().trim();
        if (Const.WriterType.JDBC.equalsIgnoreCase(type) && sink instanceof JdbcWriterVO jdbc) {
            collectJdbcSource(jdbc.getDataSourceId(), jdbc.getDataSource(), catalog, capturedAt, seen, connections);
        } else if ("KAFKA".equalsIgnoreCase(type) && sink instanceof KafkaWriterVO) {
            collectManagedRef(sink.getDataSourceId(), ConnectionKind.KAFKA, catalog, capturedAt, seen, connections);
        } else if ("ELASTICSEARCH".equalsIgnoreCase(type) && sink instanceof ElasticsearchWriterVO) {
            collectManagedRef(sink.getDataSourceId(), ConnectionKind.ELASTICSEARCH, catalog, capturedAt, seen, connections);
        }
    }

    private void collectJdbcSource(
            String dataSourceId,
            InlineDataSourceVO inline,
            ConnectionCatalog catalog,
            Instant capturedAt,
            Set<String> seen,
            List<SnapshottedConnectionRef> connections) {
        if (StrKit.isNotBlank(dataSourceId)) {
            collectManagedRef(dataSourceId.trim(), ConnectionKind.JDBC, catalog, capturedAt, seen, connections);
            return;
        }
        if (inline != null && StrKit.isNotBlank(inline.getName())) {
            addRef(seen, connections, inlineRef(inline, capturedAt));
        }
    }

    private void collectManagedRef(
            String name,
            ConnectionKind kind,
            ConnectionCatalog catalog,
            Instant capturedAt,
            Set<String> seen,
            List<SnapshottedConnectionRef> connections) {
        if (StrKit.isBlank(name)) {
            return;
        }
        String trimmed = name.trim();
        String dedupeKey = kind.name() + ":" + trimmed;
        if (!seen.add(dedupeKey)) {
            return;
        }
        CatalogEntry entry = catalog.findEntry(trimmed, kind).orElse(null);
        CatalogEntrySource source = entry != null ? entry.source() : CatalogEntrySource.BOOTSTRAP;
        long version = entry != null ? entry.version() : capturedAt.toEpochMilli();
        Instant updatedAt = entry != null ? entry.updatedAt() : capturedAt;
        Map<String, Object> params = loadManagedParams(trimmed, kind, entry);
        connections.add(new SnapshottedConnectionRef(trimmed, kind, source, version, updatedAt, params));
    }

    private Map<String, Object> loadManagedParams(String name, ConnectionKind kind, CatalogEntry entry) {
        return switch (kind) {
            case JDBC -> loadJdbcParams(name, entry);
            case KAFKA -> loadKafkaParams(name);
            case ELASTICSEARCH -> loadElasticsearchParams(name);
        };
    }

    private Map<String, Object> loadJdbcParams(String name, CatalogEntry entry) {
        return dataSourceConfigRepository.findById(name)
                .filter(row -> Boolean.TRUE.equals(row.getEnabled()))
                .map(this::jdbcParamsFromRow)
                .orElseGet(() -> bootstrapJdbcParams(name, entry));
    }

    private Map<String, Object> jdbcParamsFromRow(DataSourceConfigPO row) {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfPresent(params, "url", row.getUrl());
        putIfPresent(params, "username", row.getUsername());
        putIfPresent(params, "driverClassName", row.getDriverClassName());
        putIfPresent(params, "passwordSecretRef", row.getPasswordSecretRef());
        putIfPresent(params, "driverJarPath", row.getDriverJarPath());
        return params;
    }

    private Map<String, Object> bootstrapJdbcParams(String name, CatalogEntry entry) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (entry != null && entry.metadata() instanceof JdbcCatalogMetadata jdbc) {
            params.put("url", jdbc.jdbcUrl());
            if (StrKit.isNotBlank(jdbc.driverClassName())) {
                params.put("driverClassName", jdbc.driverClassName());
            }
        } else {
            params.put("url", name);
        }
        params.put("bootstrap", Boolean.TRUE);
        return params;
    }

    private Map<String, Object> loadKafkaParams(String name) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("cluster", name);
        messagingClusterConfigRepository.findById(name)
                .filter(row -> MessagingClusterType.KAFKA.name().equals(row.getClusterType()))
                .filter(row -> Boolean.TRUE.equals(row.getEnabled()))
                .ifPresent(row -> {
                    MessagingClusterConfigService.KafkaClusterConfig config =
                            TemplateJsonCodec.read(row.getConfigJson(), MessagingClusterConfigService.KafkaClusterConfig.class);
                    params.put("bootstrapServers", config.bootstrapServers());
                    if (StrKit.isNotBlank(config.clientId())) {
                        params.put("clientId", config.clientId());
                    }
                    if (StrKit.isNotBlank(config.securityProtocol())) {
                        params.put("securityProtocol", config.securityProtocol());
                    }
                    if (StrKit.isNotBlank(config.saslMechanism())) {
                        params.put("saslMechanism", config.saslMechanism());
                    }
                    if (StrKit.isNotBlank(config.saslJaasConfig())) {
                        params.put("hasSaslJaasConfig", Boolean.TRUE);
                    }
                });
        return params;
    }

    private Map<String, Object> loadElasticsearchParams(String name) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("cluster", name);
        messagingClusterConfigRepository.findById(name)
                .filter(row -> MessagingClusterType.ELASTICSEARCH.name().equals(row.getClusterType()))
                .filter(row -> Boolean.TRUE.equals(row.getEnabled()))
                .ifPresent(row -> {
                    MessagingClusterConfigService.ElasticsearchClusterConfig config =
                            TemplateJsonCodec.read(row.getConfigJson(), MessagingClusterConfigService.ElasticsearchClusterConfig.class);
                    params.put("hosts", config.uris());
                    if (StrKit.isNotBlank(config.username())) {
                        params.put("username", config.username());
                    }
                    if (StrKit.isNotBlank(config.pathPrefix())) {
                        params.put("pathPrefix", config.pathPrefix());
                    }
                    if (StrKit.isNotBlank(config.apiKey())) {
                        params.put("apiKeySecretRef", "managed:" + name + ":apiKey");
                    } else if (StrKit.isNotBlank(config.password())) {
                        params.put("passwordSecretRef", "managed:" + name + ":password");
                    }
                });
        return params;
    }

    private SnapshottedConnectionRef inlineRef(InlineDataSourceVO inline, Instant capturedAt) {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfPresent(params, "url", inline.getUrl());
        putIfPresent(params, "username", inline.getUsername());
        putIfPresent(params, "driverClassName", inline.getDriverClassName());
        putIfPresent(params, "passwordSecretRef", inline.getPasswordSecretRef());
        if (inline.getProperties() != null && !inline.getProperties().isEmpty()) {
            params.put("properties", Map.copyOf(inline.getProperties()));
        }
        return new SnapshottedConnectionRef(
                inline.getName().trim(),
                ConnectionKind.JDBC,
                CatalogEntrySource.MANAGED,
                capturedAt.toEpochMilli(),
                capturedAt,
                params);
    }

    private static void putIfPresent(Map<String, Object> params, String key, String value) {
        if (StrKit.isNotBlank(value)) {
            params.put(key, value);
        }
    }

    private static void addRef(
            Set<String> seen,
            List<SnapshottedConnectionRef> connections,
            SnapshottedConnectionRef ref) {
        String dedupeKey = ref.kind().name() + ":" + ref.name();
        if (seen.add(dedupeKey)) {
            connections.add(ref);
        }
    }
}
