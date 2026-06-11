package org.gensokyo.data.kafka.support;

import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicKafkaTemplateRegistry {

    private volatile String primary;
    private final ConcurrentHashMap<String, KafkaTemplate<String, String>> templates;

    public DynamicKafkaTemplateRegistry(String primary, Map<String, KafkaTemplate<String, String>> templates) {
        this.primary = primary;
        this.templates = new ConcurrentHashMap<>(templates);
    }

    /**
     * Registers or replaces a cluster template at runtime (console-managed clusters).
     *
     * @param cluster  cluster key
     * @param template producer template
     */
    public void register(String cluster, KafkaTemplate<String, String> template) {
        Objects.requireNonNull(cluster, "cluster");
        Objects.requireNonNull(template, "template");
        templates.put(cluster, template);
    }

    /**
     * Removes a cluster template from the registry.
     *
     * @param cluster cluster key
     */
    public void unregister(String cluster) {
        if (cluster != null && !cluster.isBlank()) {
            templates.remove(cluster);
        }
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
