/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ElasticsearchResolvedConnection;
import org.gensokyo.data.datasource.api.JdbcResolvedConnection;
import org.gensokyo.data.datasource.api.KafkaResolvedConnection;
import org.gensokyo.data.datasource.api.ResolvedConnection;
import org.gensokyo.data.datasource.api.snapshot.SnapshottedConnectionRef;
import org.gensokyo.data.datasource.jdbc.JdbcConnectionPoolFactory;
import org.gensokyo.data.messaging.MessagingClusterConfigService;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.secret.SecretResolver;
import org.gensokyo.kit.character.StrKit;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Materializes frozen snapshot params into runtime handles without touching the live catalog (D-01, D-07).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@Component
@RequiredArgsConstructor
public class SnapshotConnectionMaterializer {

    private final SecretResolver secretResolver;

    /**
     * Builds a kind-specific resolved handle from a snapshotted ref for an active execution.
     *
     * @param instanceId execution instance id used for isolated JDBC routing keys
     * @param ref        frozen connection reference
     * @return resolved runtime handle backed by snapshot params only
     */
    public ResolvedConnection materialize(Long instanceId, SnapshottedConnectionRef ref) {
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(ref, "ref");
        return switch (ref.kind()) {
            case JDBC -> materializeJdbc(instanceId, ref);
            case KAFKA -> materializeKafka(ref);
            case ELASTICSEARCH -> materializeElasticsearch(ref);
        };
    }

    private ResolvedConnection materializeJdbc(Long instanceId, SnapshottedConnectionRef ref) {
        Map<String, Object> params = ref.configParams();
        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName(ref.name());
        inline.setUrl(stringParam(params, "url"));
        inline.setUsername(stringParam(params, "username"));
        inline.setDriverClassName(stringParam(params, "driverClassName"));
        inline.setPasswordSecretRef(stringParam(params, "passwordSecretRef"));
        Object properties = params.get("properties");
        if (properties instanceof Map<?, ?> map) {
            Map<String, String> props = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key != null && value != null) {
                    props.put(String.valueOf(key), String.valueOf(value));
                }
            });
            inline.setProperties(props);
        }
        DruidDataSource pool = JdbcConnectionPoolFactory.createInlinePool(inline, secretResolver);
        String routingKey = SnapshotRoutingKeys.isolated(instanceId, ref.name());
        return new JdbcResolvedConnection(routingKey, pool);
    }

    private ResolvedConnection materializeKafka(SnapshottedConnectionRef ref) {
        Map<String, Object> params = ref.configParams();
        @SuppressWarnings("unchecked")
        List<String> bootstrapServers = params.get("bootstrapServers") instanceof List<?> list
                ? (List<String>) list
                : List.of(ref.name());
        MessagingClusterConfigService.KafkaClusterConfig config = new MessagingClusterConfigService.KafkaClusterConfig(
                bootstrapServers,
                stringParam(params, "clientId"),
                null,
                null,
                null,
                stringParam(params, "securityProtocol"),
                stringParam(params, "saslMechanism"),
                null,
                Map.of());
        KafkaTemplate<String, String> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config.toProducerProperties()));
        return new KafkaResolvedConnection(ref.name(), template);
    }

    private ResolvedConnection materializeElasticsearch(SnapshottedConnectionRef ref) {
        Map<String, Object> params = ref.configParams();
        @SuppressWarnings("unchecked")
        List<String> hosts = params.get("hosts") instanceof List<?> list
                ? (List<String>) list
                : List.of("http://localhost:9200");
        MessagingClusterConfigService.ElasticsearchClusterConfig config =
                new MessagingClusterConfigService.ElasticsearchClusterConfig(
                        hosts,
                        stringParam(params, "username"),
                        null,
                        null,
                        stringParam(params, "pathPrefix"),
                        null,
                        null,
                        null);
        HttpHost[] httpHosts = hosts.stream().map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(httpHosts);
        if (StrKit.isNotBlank(config.pathPrefix())) {
            builder.setPathPrefix(config.pathPrefix());
        }
        RestClient client = builder.build();
        return new ElasticsearchResolvedConnection(ref.name(), client);
    }

    private static String stringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
