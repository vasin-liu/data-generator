/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.messaging;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.gensokyo.data.api.console.dto.ElasticsearchClusterUpsertRequest;
import org.gensokyo.data.api.console.dto.KafkaClusterUpsertRequest;
import org.gensokyo.data.elasticsearch.support.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry;
import org.gensokyo.data.model.po.MessagingClusterConfigPO;
import org.gensokyo.data.repository.MessagingClusterConfigRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persists and hot-registers Kafka / Elasticsearch clusters for the operator console.
 *
 * @author Gensokyo
 * @since 2026-06-04
 */
@Service
@RequiredArgsConstructor
public class MessagingClusterConfigService {

    private final MessagingClusterConfigRepository repository;
    private final ObjectProvider<DynamicKafkaTemplateRegistry> kafkaRegistryProvider;
    private final ObjectProvider<DynamicElasticsearchClientRegistry> elasticsearchRegistryProvider;

    /**
     * Loads persisted clusters into runtime registries on startup.
     */
    @PostConstruct
    void loadPersistedClusters() {
        refreshRuntimeRegistrations();
    }

    /**
     * @return summaries of persisted Kafka clusters
     */
    public List<MessagingClusterSummary> listKafka() {
        return repository.findByClusterType(MessagingClusterType.KAFKA.name()).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * @return summaries of persisted Elasticsearch clusters
     */
    public List<MessagingClusterSummary> listElasticsearch() {
        return repository.findByClusterType(MessagingClusterType.ELASTICSEARCH.name()).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * @return merged Kafka cluster keys (yaml + persisted)
     */
    public List<String> listKafkaClusterKeys() {
        LinkedHashMap<String, Boolean> keys = new LinkedHashMap<>();
        DynamicKafkaTemplateRegistry registry = kafkaRegistryProvider.getIfAvailable();
        if (registry != null) {
            registry.getTemplates().keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        }
        return keys.keySet().stream().sorted().toList();
    }

    /**
     * @return merged Elasticsearch cluster keys (yaml + persisted)
     */
    public List<String> listElasticsearchClusterKeys() {
        LinkedHashMap<String, Boolean> keys = new LinkedHashMap<>();
        DynamicElasticsearchClientRegistry registry = elasticsearchRegistryProvider.getIfAvailable();
        if (registry != null) {
            registry.getLowLevelClients().keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        }
        return keys.keySet().stream().sorted().toList();
    }

    /**
     * @param request cluster definition
     * @return summary
     */
    @Transactional
    public MessagingClusterSummary saveKafka(KafkaClusterUpsertRequest request) {
        Objects.requireNonNull(request, "request");
        String name = requireName(request.name());
        if (request.bootstrapServers() == null || request.bootstrapServers().isEmpty()) {
            throw new IllegalArgumentException("bootstrapServers is required");
        }
        Optional<KafkaClusterConfig> existing = readKafkaConfig(name);
        String saslJaasConfig = mergeSecret(
                request.saslJaasConfig(), existing.map(KafkaClusterConfig::saslJaasConfig).orElse(null));
        KafkaClusterConfig config = new KafkaClusterConfig(
                request.bootstrapServers(),
                request.clientId(),
                request.acks(),
                request.compressionType(),
                request.retries(),
                request.securityProtocol(),
                request.saslMechanism(),
                saslJaasConfig,
                request.properties());
        MessagingClusterConfigPO entity = requireOrCreate(name);
        entity.setClusterType(MessagingClusterType.KAFKA.name());
        entity.setConfigJson(TemplateJsonCodec.write(config));
        entity.setEnabled(Boolean.TRUE);
        touch(entity);
        repository.saveAndFlush(entity);
        registerKafka(name, config);
        return toSummary(entity);
    }

    /**
     * @param request cluster definition
     * @return summary
     */
    @Transactional
    public MessagingClusterSummary saveElasticsearch(ElasticsearchClusterUpsertRequest request) {
        Objects.requireNonNull(request, "request");
        String name = requireName(request.name());
        if (request.uris() == null || request.uris().isEmpty()) {
            throw new IllegalArgumentException("uris is required");
        }
        Optional<ElasticsearchClusterConfig> existing = readElasticsearchConfig(name);
        String password = mergeSecret(
                request.password(), existing.map(ElasticsearchClusterConfig::password).orElse(null));
        String apiKey = mergeSecret(request.apiKey(), existing.map(ElasticsearchClusterConfig::apiKey).orElse(null));
        ElasticsearchClusterConfig config = new ElasticsearchClusterConfig(
                request.uris(),
                request.username(),
                password,
                apiKey,
                request.pathPrefix(),
                request.connectionTimeoutMs(),
                request.socketTimeoutMs(),
                request.socketKeepAlive());
        MessagingClusterConfigPO entity = requireOrCreate(name);
        entity.setClusterType(MessagingClusterType.ELASTICSEARCH.name());
        entity.setConfigJson(TemplateJsonCodec.write(config));
        entity.setEnabled(Boolean.TRUE);
        touch(entity);
        repository.saveAndFlush(entity);
        registerElasticsearch(name, config);
        return toSummary(entity);
    }

    /**
     * @param name cluster id
     */
    @Transactional
    public void remove(String name) {
        Objects.requireNonNull(name, "name");
        MessagingClusterConfigPO entity = repository.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + name));
        repository.delete(entity);
        unregister(entity);
    }

    private void refreshRuntimeRegistrations() {
        for (MessagingClusterConfigPO row : repository.findAll()) {
            if (!Boolean.TRUE.equals(row.getEnabled())) {
                continue;
            }
            try {
                if (MessagingClusterType.KAFKA.name().equals(row.getClusterType())) {
                    registerKafka(row.getName(), TemplateJsonCodec.read(row.getConfigJson(), KafkaClusterConfig.class));
                } else if (MessagingClusterType.ELASTICSEARCH.name().equals(row.getClusterType())) {
                    registerElasticsearch(
                            row.getName(),
                            TemplateJsonCodec.read(row.getConfigJson(), ElasticsearchClusterConfig.class));
                }
            } catch (Exception ex) {
                throw new DataGeneratorException("Failed to register messaging cluster: " + row.getName(), ex);
            }
        }
    }

    private void registerKafka(String name, KafkaClusterConfig config) {
        DynamicKafkaTemplateRegistry registry = kafkaRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        KafkaTemplate<String, String> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config.toProducerProperties()));
        registry.register(name, template);
    }

    private void registerElasticsearch(String name, ElasticsearchClusterConfig config) {
        DynamicElasticsearchClientRegistry registry = elasticsearchRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        try {
            registry.register(name, buildRestClient(config));
        } catch (IOException ex) {
            throw new DataGeneratorException("Failed to register Elasticsearch cluster: " + name, ex);
        }
    }

    private void unregister(MessagingClusterConfigPO entity) {
        try {
            if (MessagingClusterType.KAFKA.name().equals(entity.getClusterType())) {
                DynamicKafkaTemplateRegistry registry = kafkaRegistryProvider.getIfAvailable();
                if (registry != null) {
                    registry.unregister(entity.getName());
                }
            } else if (MessagingClusterType.ELASTICSEARCH.name().equals(entity.getClusterType())) {
                DynamicElasticsearchClientRegistry registry = elasticsearchRegistryProvider.getIfAvailable();
                if (registry != null) {
                    registry.unregister(entity.getName());
                }
            }
        } catch (IOException ex) {
            throw new DataGeneratorException("Failed to unregister cluster: " + entity.getName(), ex);
        }
    }

    private RestClient buildRestClient(ElasticsearchClusterConfig config) {
        HttpHost[] hosts = config.uris().stream().map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(hosts);
        if (StringUtils.hasText(config.pathPrefix())) {
            builder.setPathPrefix(config.pathPrefix());
        }
        builder.setRequestConfigCallback(requestBuilder -> {
            if (config.connectionTimeoutMs() != null) {
                requestBuilder.setConnectTimeout(config.connectionTimeoutMs());
            }
            if (config.socketTimeoutMs() != null) {
                requestBuilder.setSocketTimeout(config.socketTimeoutMs());
            }
            return requestBuilder;
        });
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            if (StringUtils.hasText(config.username())) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(config.username(), config.password()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            if (StringUtils.hasText(config.apiKey())) {
                httpClientBuilder.setDefaultHeaders(List.of(
                        (Header) new BasicHeader(
                                HttpHeaders.AUTHORIZATION, "ApiKey " + encodeApiKey(config.apiKey()))));
            }
            if (Boolean.TRUE.equals(config.socketKeepAlive())) {
                httpClientBuilder.setKeepAliveStrategy((response, context) -> 60_000);
            }
            return httpClientBuilder;
        });
        return builder.build();
    }

    private static String encodeApiKey(String apiKey) {
        return Base64.getEncoder().encodeToString(apiKey.getBytes(StandardCharsets.UTF_8));
    }

    private MessagingClusterConfigPO requireOrCreate(String name) {
        return repository.findById(name).orElseGet(() -> {
            MessagingClusterConfigPO created = new MessagingClusterConfigPO();
            created.setName(name);
            created.setCreatedAt(Instant.now());
            return created;
        });
    }

    private void touch(MessagingClusterConfigPO entity) {
        Instant now = Instant.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private Optional<KafkaClusterConfig> readKafkaConfig(String name) {
        return repository.findById(name)
                .filter(row -> MessagingClusterType.KAFKA.name().equals(row.getClusterType()))
                .map(row -> TemplateJsonCodec.read(row.getConfigJson(), KafkaClusterConfig.class));
    }

    private Optional<ElasticsearchClusterConfig> readElasticsearchConfig(String name) {
        return repository.findById(name)
                .filter(row -> MessagingClusterType.ELASTICSEARCH.name().equals(row.getClusterType()))
                .map(row -> TemplateJsonCodec.read(row.getConfigJson(), ElasticsearchClusterConfig.class));
    }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Cluster name is required");
        }
        return name;
    }

    private static String mergeSecret(String incoming, String existing) {
        return StringUtils.hasText(incoming) ? incoming : existing;
    }

    private MessagingClusterSummary toSummary(MessagingClusterConfigPO entity) {
        if (!StringUtils.hasText(entity.getConfigJson())) {
            return new MessagingClusterSummary(
                    entity.getName(),
                    entity.getClusterType(),
                    entity.getEnabled(),
                    entity.getUpdatedAt(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        if (MessagingClusterType.KAFKA.name().equals(entity.getClusterType())) {
            KafkaClusterConfig config = TemplateJsonCodec.read(entity.getConfigJson(), KafkaClusterConfig.class);
            return new MessagingClusterSummary(
                    entity.getName(),
                    entity.getClusterType(),
                    entity.getEnabled(),
                    entity.getUpdatedAt(),
                    config.bootstrapServers(),
                    null,
                    null,
                    config.clientId(),
                    config.acks(),
                    config.compressionType(),
                    config.retries(),
                    config.securityProtocol(),
                    config.saslMechanism(),
                    config.properties(),
                    null,
                    null,
                    null,
                    null,
                    StringUtils.hasText(config.saslJaasConfig()),
                    null,
                    null);
        }
        ElasticsearchClusterConfig config =
                TemplateJsonCodec.read(entity.getConfigJson(), ElasticsearchClusterConfig.class);
        return new MessagingClusterSummary(
                entity.getName(),
                entity.getClusterType(),
                entity.getEnabled(),
                entity.getUpdatedAt(),
                null,
                config.uris(),
                config.username(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                config.pathPrefix(),
                config.connectionTimeoutMs(),
                config.socketTimeoutMs(),
                config.socketKeepAlive(),
                null,
                StringUtils.hasText(config.password()),
                StringUtils.hasText(config.apiKey()));
    }

    /**
     * Persisted Kafka cluster JSON shape.
     */
    public record KafkaClusterConfig(
            List<String> bootstrapServers,
            String clientId,
            String acks,
            String compressionType,
            Integer retries,
            String securityProtocol,
            String saslMechanism,
            String saslJaasConfig,
            Map<String, String> properties) {

        /**
         * Normalizes nullable collections for backward-compatible JSON.
         */
        public KafkaClusterConfig {
            if (properties == null) {
                properties = Map.of();
            }
        }

        /**
         * @return Kafka producer configuration map
         */
        public Map<String, Object> toProducerProperties() {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            if (StringUtils.hasText(clientId)) {
                config.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId);
            }
            if (StringUtils.hasText(acks)) {
                config.put(ProducerConfig.ACKS_CONFIG, acks);
            }
            if (StringUtils.hasText(compressionType)) {
                config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
            }
            if (retries != null) {
                config.put(ProducerConfig.RETRIES_CONFIG, retries);
            }
            Map<String, String> merged = new LinkedHashMap<>(properties);
            if (StringUtils.hasText(securityProtocol)) {
                merged.put("security.protocol", securityProtocol);
            }
            if (StringUtils.hasText(saslMechanism)) {
                merged.put("sasl.mechanism", saslMechanism);
            }
            if (StringUtils.hasText(saslJaasConfig)) {
                merged.put("sasl.jaas.config", saslJaasConfig);
            }
            config.putAll(merged);
            config.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            config.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return config;
        }
    }

    /**
     * Persisted Elasticsearch cluster JSON shape.
     */
    public record ElasticsearchClusterConfig(
            List<String> uris,
            String username,
            String password,
            String apiKey,
            String pathPrefix,
            Integer connectionTimeoutMs,
            Integer socketTimeoutMs,
            Boolean socketKeepAlive) {
    }

    /**
     * Console list row for a messaging cluster.
     */
    public record MessagingClusterSummary(
            String name,
            String clusterType,
            Boolean enabled,
            Instant updatedAt,
            List<String> bootstrapServers,
            List<String> uris,
            String username,
            String clientId,
            String acks,
            String compressionType,
            Integer retries,
            String securityProtocol,
            String saslMechanism,
            Map<String, String> properties,
            String pathPrefix,
            Integer connectionTimeoutMs,
            Integer socketTimeoutMs,
            Boolean socketKeepAlive,
            Boolean hasSaslJaasConfig,
            Boolean hasPassword,
            Boolean hasApiKey) {
    }
}
