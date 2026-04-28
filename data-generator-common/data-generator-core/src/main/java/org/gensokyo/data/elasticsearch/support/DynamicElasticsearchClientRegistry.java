package org.gensokyo.data.elasticsearch.support;

import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class DynamicElasticsearchClientRegistry implements DisposableBean {

    private final String primary;
    private final Map<String, RestClient> lowLevelClients;

    public DynamicElasticsearchClientRegistry(String primary, Map<String, RestClient> lowLevelClients) {
        this.primary = primary;
        this.lowLevelClients = Map.copyOf(lowLevelClients);
    }

    public RestClient llc(String cluster) {
        String resolvedCluster = resolveCluster(cluster);
        RestClient client = lowLevelClients.get(resolvedCluster);
        if (client == null) {
            throw new IllegalArgumentException("Unknown Elasticsearch cluster: " + resolvedCluster);
        }
        return client;
    }

    public boolean hasCluster(String cluster) {
        return lowLevelClients.containsKey(cluster);
    }

    public String getPrimary() {
        return primary;
    }

    public Map<String, RestClient> getLowLevelClients() {
        return Collections.unmodifiableMap(lowLevelClients);
    }

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
