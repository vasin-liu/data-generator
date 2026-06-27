/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.gensokyo.data.datasource.api.ConnectionTestResult;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Kafka cluster connectivity probe via AdminClient metadata (D-18, D-20).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public final class KafkaConnectivityTester {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private KafkaConnectivityTester() {
    }

    /**
     * Pings a Kafka cluster by listing topics with a short timeout.
     *
     * @param bootstrapServers broker bootstrap list
     * @param extraProperties  optional security/protocol properties (no secret values in returned details)
     * @return actionable success or failure result
     */
    public static ConnectionTestResult test(List<String> bootstrapServers, Map<String, String> extraProperties) {
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            return ConnectionTestResult.fail("bootstrapServers is required");
        }
        String brokers = String.join(",", bootstrapServers);
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) TIMEOUT.toMillis());
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) TIMEOUT.toMillis());
        if (extraProperties != null) {
            extraProperties.forEach(props::put);
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("bootstrapServers", brokers);
        try (AdminClient admin = AdminClient.create(props)) {
            admin.listTopics(new ListTopicsOptions().timeoutMs((int) TIMEOUT.toMillis())).names().get();
            return ConnectionTestResult.ok("Kafka cluster reachable", details);
        } catch (Exception ex) {
            String message = summarizeFailure(ex);
            return ConnectionTestResult.fail(message, details);
        }
    }

    private static String summarizeFailure(Exception ex) {
        String root = ex.getMessage();
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            root = ex.getCause().getMessage();
        }
        if (root == null || root.isBlank()) {
            return "Kafka cluster unreachable — verify bootstrap servers and network access";
        }
        String lower = root.toLowerCase();
        if (lower.contains("authentication") || lower.contains("sasl")) {
            return "Kafka authentication failed — verify security protocol and SASL settings";
        }
        if (lower.contains("connection") || lower.contains("timeout") || lower.contains("refused")) {
            return "Kafka broker unreachable — verify bootstrap servers and firewall rules";
        }
        return "Kafka connectivity test failed: " + truncate(root);
    }

    private static String truncate(String message) {
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
