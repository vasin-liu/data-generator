/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.kafka;

import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry of named Kafka producer templates with primary-cluster fallback (D-04).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public class DynamicKafkaTemplateRegistry {

    private volatile String primary;
    private final ConcurrentHashMap<String, KafkaTemplate<String, String>> templates;

    /**
     * Creates a registry from bootstrap cluster definitions.
     *
     * @param primary   default cluster key when callers pass a blank cluster name
     * @param templates initial cluster-to-template map
     */
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

    /**
     * Resolves a producer template for the given cluster, falling back to primary when blank.
     *
     * @param cluster cluster key or blank for primary
     * @return resolved Kafka template
     * @throws IllegalArgumentException when the resolved cluster is unknown
     */
    public KafkaTemplate<String, String> template(String cluster) {
        String resolvedCluster = resolveCluster(cluster);
        KafkaTemplate<String, String> template = templates.get(resolvedCluster);
        if (template == null) {
            throw new IllegalArgumentException("Unknown Kafka cluster: " + resolvedCluster);
        }
        return template;
    }

    /**
     * Returns whether a template exists for the exact cluster key (no primary fallback).
     *
     * @param cluster cluster key
     * @return true when a template is registered under the key
     */
    public boolean hasTemplate(String cluster) {
        return templates.containsKey(cluster);
    }

    /**
     * Returns the configured primary cluster key.
     *
     * @return primary cluster name
     */
    public String getPrimary() {
        return primary;
    }

    /**
     * Returns an unmodifiable view of registered templates.
     *
     * @return cluster-to-template map snapshot
     */
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
