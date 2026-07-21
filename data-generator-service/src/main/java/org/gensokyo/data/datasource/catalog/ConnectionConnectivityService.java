/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.datasource.DataSourceDriverSupport;
import org.gensokyo.data.datasource.JdbcDriverLoadResult;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.ConnectionTestRequest;
import org.gensokyo.data.datasource.api.ConnectionTestResult;
import org.gensokyo.data.datasource.elasticsearch.ElasticsearchConnectivityTester;
import org.gensokyo.data.datasource.kafka.KafkaConnectivityTester;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.messaging.MessagingClusterConfigService;
import org.gensokyo.data.messaging.MessagingClusterConfigService.ElasticsearchClusterConfig;
import org.gensokyo.data.messaging.MessagingClusterConfigService.KafkaClusterConfig;
import org.gensokyo.data.messaging.MessagingClusterType;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.model.po.MessagingClusterConfigPO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.data.repository.MessagingClusterConfigRepository;
import org.gensokyo.data.secret.SecretResolver;
import org.gensokyo.kit.character.StrKit;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kind-specific connectivity probes for {@link ConnectionCatalogImpl#test(ConnectionTestRequest)} (D-18).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@Service
@RequiredArgsConstructor
public class ConnectionConnectivityService {

    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final MessagingClusterConfigRepository messagingClusterConfigRepository;
    private final DataSourceDriverSupport driverSupport;
    private final SecretResolver secretResolver;

    /**
     * Tests connectivity for a named catalog entry or draft payload.
     *
     * @param request test input
     * @return actionable result without secret values in details
     */
    public ConnectionTestResult test(ConnectionTestRequest request) {
        Objects.requireNonNull(request, "request");
        return request.isExistingEntry()
                ? testExisting(request.kind(), request.name())
                : testDraft(request.kind(), request.draftPayload());
    }

    private ConnectionTestResult testExisting(ConnectionKind kind, String name) {
        return switch (kind) {
            case JDBC -> testExistingJdbc(name);
            case KAFKA -> testExistingKafka(name);
            case ELASTICSEARCH -> testExistingElasticsearch(name);
        };
    }

    private ConnectionTestResult testDraft(ConnectionKind kind, Map<String, Object> payload) {
        return switch (kind) {
            case JDBC -> testDraftJdbc(payload);
            case KAFKA -> testDraftKafka(payload);
            case ELASTICSEARCH -> testDraftElasticsearch(payload);
        };
    }

    private ConnectionTestResult testExistingJdbc(String name) {
        DataSourceConfigPO row = dataSourceConfigRepository.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown JDBC datasource: " + name));
        return testJdbc(
                row.getUrl(),
                row.getUsername(),
                secretResolver.resolveInlinePassword(row.getPassword(), row.getPasswordSecretRef()),
                row.getDriverClassName(),
                row.getDriverJarPath());
    }

    private ConnectionTestResult testDraftJdbc(Map<String, Object> payload) {
        String url = stringField(payload, "url");
        String username = stringField(payload, "username");
        String password = resolveDraftPassword(payload);
        String driverClassName = stringField(payload, "driverClassName");
        String driverJarPath = stringField(payload, "driverJarPath");
        if (StrKit.isBlank(url)) {
            return ConnectionTestResult.fail("JDBC url is required");
        }
        if (StrKit.isBlank(driverClassName)) {
            return ConnectionTestResult.fail("JDBC driverClassName is required");
        }
        return testJdbc(url, username, password, driverClassName, driverJarPath);
    }

    private ConnectionTestResult testJdbc(
            String url,
            String username,
            String password,
            String driverClassName,
            String driverJarPath) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("driverClassName", driverClassName);
        try {
            JdbcDriverLoadResult loaded = driverSupport.ensureDriverLoaded(driverClassName, url, driverJarPath);
            try (Connection connection = driverSupport.openConnection(url, username, password, loaded)) {
                if (!connection.isValid(5)) {
                    return ConnectionTestResult.fail("JDBC connection invalid — verify URL and credentials", details);
                }
            }
            return ConnectionTestResult.ok("JDBC connection OK", details);
        } catch (DataGeneratorException ex) {
            return ConnectionTestResult.fail(
                    sanitizeOperatorMessage("JDBC connectivity test failed: " + ex.getMessage(), password, url),
                    enrichFailureDetails(details, driverClassName));
        } catch (Exception ex) {
            return ConnectionTestResult.fail(
                    summarizeJdbcFailure(ex, password, url, driverClassName), enrichFailureDetails(details, driverClassName));
        }
    }

    private ConnectionTestResult testExistingKafka(String name) {
        MessagingClusterConfigPO row = messagingClusterConfigRepository.findById(name)
                .filter(r -> MessagingClusterType.KAFKA.name().equals(r.getClusterType()))
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown Kafka cluster: " + name));
        KafkaClusterConfig config = TemplateJsonCodec.read(row.getConfigJson(), KafkaClusterConfig.class);
        return KafkaConnectivityTester.test(config.bootstrapServers(), securityProperties(config));
    }

    private ConnectionTestResult testDraftKafka(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<String> bootstrapServers = payload.get("bootstrapServers") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : null;
        @SuppressWarnings("unchecked")
        Map<String, String> properties = payload.get("properties") instanceof Map<?, ?> map
                ? map.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                        e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()), (a, b) -> b, LinkedHashMap::new))
                : new LinkedHashMap<>();
        addIfPresent(properties, payload, "securityProtocol", "security.protocol");
        addIfPresent(properties, payload, "saslMechanism", "sasl.mechanism");
        return KafkaConnectivityTester.test(bootstrapServers, properties);
    }

    private ConnectionTestResult testExistingElasticsearch(String name) {
        MessagingClusterConfigPO row = messagingClusterConfigRepository.findById(name)
                .filter(r -> MessagingClusterType.ELASTICSEARCH.name().equals(r.getClusterType()))
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown Elasticsearch cluster: " + name));
        ElasticsearchClusterConfig config = TemplateJsonCodec.read(row.getConfigJson(), ElasticsearchClusterConfig.class);
        return ElasticsearchConnectivityTester.test(
                config.uris(), config.username(), config.password(), config.pathPrefix());
    }

    private ConnectionTestResult testDraftElasticsearch(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<String> uris = payload.get("uris") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : null;
        String username = stringField(payload, "username");
        String password = stringField(payload, "password");
        String pathPrefix = stringField(payload, "pathPrefix");
        return ElasticsearchConnectivityTester.test(uris, username, password, pathPrefix);
    }

    private static Map<String, String> securityProperties(KafkaClusterConfig config) {
        Map<String, String> props = new LinkedHashMap<>(config.properties());
        if (StrKit.isNotBlank(config.securityProtocol())) {
            props.put("security.protocol", config.securityProtocol());
        }
        if (StrKit.isNotBlank(config.saslMechanism())) {
            props.put("sasl.mechanism", config.saslMechanism());
        }
        if (StrKit.isNotBlank(config.saslJaasConfig())) {
            props.put("sasl.jaas.config", config.saslJaasConfig());
        }
        return props;
    }

    private static void addIfPresent(
            Map<String, String> target,
            Map<String, Object> payload,
            String sourceKey,
            String kafkaKey) {
        String value = stringField(payload, sourceKey);
        if (StrKit.isNotBlank(value)) {
            target.put(kafkaKey, value);
        }
    }

    private static String stringField(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String resolveDraftPassword(Map<String, Object> payload) {
        String secretRef = stringField(payload, "passwordSecretRef");
        if (StrKit.isNotBlank(secretRef)) {
            return secretResolver.resolveInlinePassword(null, secretRef);
        }
        return stringField(payload, "password");
    }

    private static String summarizeJdbcFailure(
            Exception ex, String password, String url, String driverClassName) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return actionableDriverHint(driverClassName)
                    + "JDBC connection failed — verify URL, driver, and credentials";
        }
        String lower = message.toLowerCase();
        if (lower.contains("password") || lower.contains("access denied") || lower.contains("authentication")) {
            return actionableDriverHint(driverClassName)
                    + "JDBC authentication failed — verify username and password";
        }
        if (lower.contains("unknown host") || lower.contains("connection refused") || lower.contains("timeout")) {
            return actionableDriverHint(driverClassName)
                    + "JDBC host unreachable — verify URL and network access";
        }
        return actionableDriverHint(driverClassName)
                + "JDBC connectivity test failed: "
                + sanitizeOperatorMessage(message, password, url);
    }

    private static Map<String, Object> enrichFailureDetails(
            Map<String, Object> details, String driverClassName) {
        if (isProprietaryPhase9Driver(driverClassName)) {
            details.put("driverClassName", driverClassName);
        }
        return details;
    }

    private static boolean isProprietaryPhase9Driver(String driverClassName) {
        if (driverClassName == null || driverClassName.isBlank()) {
            return false;
        }
        return driverClassName.startsWith("dm.jdbc.")
                || driverClassName.startsWith("com.kingbase")
                || driverClassName.startsWith("com.highgo.");
    }

    private static String actionableDriverHint(String driverClassName) {
        if (!isProprietaryPhase9Driver(driverClassName)) {
            return "";
        }
        return "[" + driverClassName + "] ";
    }

    /**
     * Removes password literals and JDBC URL userinfo from operator-facing messages (D-11).
     */
    private static String sanitizeOperatorMessage(String message, String password, String jdbcUrl) {
        if (message == null) {
            return "";
        }
        String sanitized = sanitizeMessage(message);
        if (password != null && !password.isBlank()) {
            sanitized = sanitized.replace(password, "[redacted]");
        }
        if (jdbcUrl != null && !jdbcUrl.isBlank() && sanitized.contains(jdbcUrl)) {
            sanitized = sanitized.replace(jdbcUrl, sanitizeMessage(jdbcUrl));
        }
        return sanitized;
    }

    private static String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        // Strip userinfo from JDBC URLs that may appear in driver error messages (D-11).
        String sanitized = message.replaceAll("(?i)jdbc:[^:@/\\s]+://[^@/\\s]+@", "jdbc://[redacted]@");
        // Redact common password query parameters echoed by drivers.
        sanitized = sanitized.replaceAll("(?i)(password=)[^&\\s\"']+", "$1[redacted]");
        sanitized = sanitized.replaceAll("(?i)(pwd=)[^&\\s\"']+", "$1[redacted]");
        return sanitized;
    }
}
