/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.gensokyo.data.datasource.api.ConnectionTestResult;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch cluster connectivity probe via HTTP ping (D-18, D-20).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public final class ElasticsearchConnectivityTester {

    private ElasticsearchConnectivityTester() {
    }

    /**
     * Pings an Elasticsearch cluster with {@code GET /} or {@code GET /_cluster/health}.
     *
     * @param uris         cluster URIs (host:port strings)
     * @param username     optional basic-auth username
     * @param password     optional basic-auth password (never echoed in details)
     * @param pathPrefix   optional path prefix
     * @return actionable success or failure result
     */
    public static ConnectionTestResult test(
            List<String> uris,
            String username,
            String password,
            String pathPrefix) {
        if (uris == null || uris.isEmpty()) {
            return ConnectionTestResult.fail("uris is required");
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("hosts", String.join(",", uris));
        HttpHost[] hosts = uris.stream().map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(hosts);
        if (pathPrefix != null && !pathPrefix.isBlank()) {
            builder.setPathPrefix(pathPrefix.trim());
        }
        if (username != null && !username.isBlank()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password == null ? "" : password));
            builder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        try (RestClient client = builder.build()) {
            Response response = client.performRequest(new Request("GET", "/"));
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                return ConnectionTestResult.ok("Elasticsearch cluster reachable", details);
            }
            return ConnectionTestResult.fail(
                    "Elasticsearch returned HTTP " + status + " — verify cluster URI and credentials", details);
        } catch (IOException ex) {
            return ConnectionTestResult.fail(summarizeFailure(ex), details);
        }
    }

    private static String summarizeFailure(IOException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Elasticsearch cluster unreachable — verify host URIs and network access";
        }
        String lower = message.toLowerCase();
        if (lower.contains("401") || lower.contains("403") || lower.contains("unauthorized")) {
            return "Elasticsearch authentication failed — verify username/password or API key";
        }
        if (lower.contains("connection") || lower.contains("timeout") || lower.contains("refused")) {
            return "Elasticsearch host unreachable — verify URIs and firewall rules";
        }
        return "Elasticsearch connectivity test failed: " + truncate(message);
    }

    private static String truncate(String message) {
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
