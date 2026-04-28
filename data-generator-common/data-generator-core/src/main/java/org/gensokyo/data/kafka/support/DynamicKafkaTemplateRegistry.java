package org.gensokyo.data.kafka.support;

import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class DynamicKafkaTemplateRegistry {

    private final String primary;
    private final Map<String, KafkaTemplate<String, String>> templates;

    public DynamicKafkaTemplateRegistry(String primary, Map<String, KafkaTemplate<String, String>> templates) {
        this.primary = primary;
        this.templates = Map.copyOf(templates);
    }

    public KafkaTemplate<String, String> template(String cluster) {
        String resolvedCluster = resolveCluster(cluster);
        KafkaTemplate<String, String> template = templates.get(resolvedCluster);
        if (template == null) {
            throw new IllegalArgumentException("Unknown Kafka cluster: " + resolvedCluster);
        }
        return template;
    }

    public boolean hasTemplate(String cluster) {
        return templates.containsKey(cluster);
    }

    public String getPrimary() {
        return primary;
    }

    public Map<String, KafkaTemplate<String, String>> getTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    private String resolveCluster(String cluster) {
        if (cluster != null && !cluster.isBlank()) {
            return cluster;
        }
        return Objects.requireNonNull(primary, "No primary Kafka cluster configured");
    }
}
