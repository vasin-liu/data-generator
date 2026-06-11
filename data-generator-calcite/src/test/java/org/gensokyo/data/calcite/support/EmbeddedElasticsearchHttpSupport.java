/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-process HTTP server that accepts Elasticsearch {@code POST /_bulk} requests for integration tests.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class EmbeddedElasticsearchHttpSupport implements AutoCloseable {

    private final List<String> bulkBodies = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private int port;

    /**
     * Starts the HTTP server on an ephemeral port with a {@code /_bulk} handler.
     *
     * @throws IOException when the server cannot bind
     */
    public void start() throws IOException {
        bulkBodies.clear();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/_bulk", this::handleBulk);
        server.start();
        port = server.getAddress().getPort();
    }

    /**
     * Returns a {@link RestClient} targeting this in-process server.
     *
     * @return low-level Elasticsearch REST client
     */
    public RestClient restClient() {
        return RestClient.builder(new HttpHost("localhost", port)).build();
    }

    /**
     * Returns captured bulk request bodies (one entry per {@code POST /_bulk}).
     *
     * @return request bodies
     */
    public List<String> bulkBodies() {
        return List.copyOf(bulkBodies);
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handleBulk(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        bulkBodies.add(body);
        int documentCount = countBulkDocuments(body);
        String response = bulkSuccessResponse(documentCount);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static int countBulkDocuments(String body) {
        if (body == null || body.isBlank()) {
            return 0;
        }
        int lines = 0;
        for (int i = 0; i < body.length(); i++) {
            if (body.charAt(i) == '\n') {
                lines++;
            }
        }
        // NDJSON: action line + document line per row
        return Math.max(0, lines / 2);
    }

    private static String bulkSuccessResponse(int documentCount) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < documentCount; i++) {
            items.add("{\"index\":{\"status\":201}}");
        }
        return "{\"errors\":false,\"items\":[" + String.join(",", items) + "]}";
    }
}
