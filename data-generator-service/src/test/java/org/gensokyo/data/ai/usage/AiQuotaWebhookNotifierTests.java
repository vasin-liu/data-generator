/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link AiQuotaWebhookNotifier}.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
class AiQuotaWebhookNotifierTests {

    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        bodies.clear();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/quota-hook", this::handleHook);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void postsWarnPayloadToConfiguredEndpoint() throws Exception {
        DataGeneratorProperties properties = new DataGeneratorProperties();
        DataGeneratorProperties.AiRuntimeQuota quota = properties.getAiRuntime().getQuota();
        quota.setWebhooksEnabled(true);
        DataGeneratorProperties.AiQuotaWebhookEndpoint endpoint = new DataGeneratorProperties.AiQuotaWebhookEndpoint();
        endpoint.setUrl("http://localhost:" + port + "/quota-hook");
        endpoint.setSecretHeaderName("X-Quota-Secret");
        endpoint.setSecretValue("test-secret");
        endpoint.setEvents(List.of("WARN"));
        quota.setWebhooks(List.of(endpoint));

        AiQuotaWebhookNotifier notifier = AiQuotaWebhookNotifier.forTests(properties, HttpClient.newHttpClient());
        notifier.notifyEvent(new AiQuotaAlertEvent(
                "WARN", "2026-06-17", "platform", "PLATFORM", "CALLS", 4.0D, 5.0D, 80.0D));

        awaitBodies(1);
        Map<?, ?> payload = TemplateJsonCodec.read(bodies.getFirst(), Map.class);
        Assertions.assertEquals("WARN", payload.get("event"));
        Assertions.assertEquals("platform", payload.get("scopeKey"));
        Assertions.assertEquals(80.0D, ((Number) payload.get("percentUsed")).doubleValue(), 0.001D);
    }

    @Test
    void skipsEndpointsThatDoNotSubscribeToEvent() throws Exception {
        DataGeneratorProperties properties = new DataGeneratorProperties();
        DataGeneratorProperties.AiRuntimeQuota quota = properties.getAiRuntime().getQuota();
        quota.setWebhooksEnabled(true);
        DataGeneratorProperties.AiQuotaWebhookEndpoint endpoint = new DataGeneratorProperties.AiQuotaWebhookEndpoint();
        endpoint.setUrl("http://localhost:" + port + "/quota-hook");
        endpoint.setEvents(List.of("EXCEEDED"));
        quota.setWebhooks(List.of(endpoint));

        AiQuotaWebhookNotifier notifier = AiQuotaWebhookNotifier.forTests(properties, HttpClient.newHttpClient());
        notifier.notifyEvent(new AiQuotaAlertEvent(
                "WARN", "2026-06-17", "platform", "PLATFORM", "CALLS", 4.0D, 5.0D, 80.0D));

        Thread.sleep(200L);
        Assertions.assertTrue(bodies.isEmpty());
    }

    private void handleHook(HttpExchange exchange) throws IOException {
        bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private void awaitBodies(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (bodies.size() >= expected) {
                return;
            }
            Thread.sleep(50L);
        }
        Assertions.assertEquals(expected, bodies.size());
    }
}
