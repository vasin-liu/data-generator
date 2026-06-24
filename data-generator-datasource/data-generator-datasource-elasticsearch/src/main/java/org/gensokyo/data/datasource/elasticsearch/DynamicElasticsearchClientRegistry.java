/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.elasticsearch;

import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry of named Elasticsearch REST clients with primary-cluster fallback (D-04).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public class DynamicElasticsearchClientRegistry implements DisposableBean {

    private volatile String primary;
    private final ConcurrentHashMap<String, RestClient> lowLevelClients;

    /**
     * Creates a registry from bootstrap cluster definitions.
     *
     * @param primary         default cluster key when callers pass a blank cluster name
     * @param lowLevelClients initial cluster-to-client map
     */
    public DynamicElasticsearchClientRegistry(String primary, Map<String, RestClient> lowLevelClients) {
        this.primary = primary;
        this.lowLevelClients = new ConcurrentHashMap<>(lowLevelClients);
    }

    /**
     * Registers or replaces a low-level REST client (closes the previous client for the same key).
     *
     * @param cluster cluster key
     * @param client  REST client
     * @throws IOException when closing a replaced client fails
     */
    public void register(String cluster, RestClient client) throws IOException {
        Objects.requireNonNull(cluster, "cluster");
        Objects.requireNonNull(client, "client");
        RestClient previous = lowLevelClients.put(cluster, client);
        if (previous != null && previous != client) {
            previous.close();
        }
    }

    /**
     * Removes and closes a cluster client.
     *
     * @param cluster cluster key
     * @throws IOException when closing the removed client fails
     */
    public void unregister(String cluster) throws IOException {
        if (cluster == null || cluster.isBlank()) {
            return;
        }
        RestClient removed = lowLevelClients.remove(cluster);
        if (removed != null) {
            removed.close();
        }
    }

    /**
     * Resolves a low-level REST client for the given cluster, falling back to primary when blank.
     *
     * @param cluster cluster key or blank for primary
     * @return resolved REST client
     * @throws IllegalArgumentException when the resolved cluster is unknown
     */
    public RestClient llc(String cluster) {
        String resolvedCluster = resolveCluster(cluster);
        RestClient client = lowLevelClients.get(resolvedCluster);
        if (client == null) {
            throw new IllegalArgumentException("Unknown Elasticsearch cluster: " + resolvedCluster);
        }
        return client;
    }

    /**
     * Returns whether a client exists for the exact cluster key (no primary fallback).
     *
     * @param cluster cluster key
     * @return true when a client is registered under the key
     */
    public boolean hasCluster(String cluster) {
        return lowLevelClients.containsKey(cluster);
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
     * Returns an unmodifiable view of registered clients.
     *
     * @return cluster-to-client map snapshot
     */
    public Map<String, RestClient> getLowLevelClients() {
        return Collections.unmodifiableMap(lowLevelClients);
    }

    /**
     * Closes all registered clients on container shutdown.
     *
     * @throws IOException when closing a client fails
     */
    @Override
    public void destroy() throws IOException {
        IOException failure = null;
        for (RestClient client : lowLevelClients.values()) {
            try {
                client.close();
            } catch (IOException ex) {
                failure = ex;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private String resolveCluster(String cluster) {
        if (cluster != null && !cluster.isBlank()) {
            return cluster;
        }
        return Objects.requireNonNull(primary, "No primary Elasticsearch cluster configured");
    }
}
