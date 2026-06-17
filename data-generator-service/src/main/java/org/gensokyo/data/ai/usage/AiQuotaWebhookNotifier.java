/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fire-and-forget HTTP webhook delivery for AI quota warn and exceed events.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
@Service
public class AiQuotaWebhookNotifier {

    private final DataGeneratorProperties properties;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    /**
     * @param properties platform configuration
     */
    @Autowired
    public AiQuotaWebhookNotifier(DataGeneratorProperties properties) {
        this(properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    /**
     * @param properties platform configuration
     * @param httpClient HTTP client for webhook delivery
     */
    private AiQuotaWebhookNotifier(DataGeneratorProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Creates a notifier for tests with a custom HTTP client.
     *
     * @param properties platform configuration
     * @param httpClient HTTP client for webhook delivery
     * @return notifier instance
     */
    static AiQuotaWebhookNotifier forTests(DataGeneratorProperties properties, HttpClient httpClient) {
        return new AiQuotaWebhookNotifier(properties, httpClient);
    }

    /**
     * Dispatches a quota alert to configured webhook endpoints when enabled.
     *
     * @param event warn or exceed payload
     */
    public void notifyEvent(AiQuotaAlertEvent event) {
        DataGeneratorProperties.AiRuntimeQuota quota = quotaConfig();
        if (!quota.isWebhooksEnabled() || quota.getWebhooks() == null || quota.getWebhooks().isEmpty()) {
            return;
        }
        for (DataGeneratorProperties.AiQuotaWebhookEndpoint endpoint : quota.getWebhooks()) {
            if (endpoint == null || !StringUtils.hasText(endpoint.getUrl())) {
                continue;
            }
            if (!accepts(endpoint, event.eventType())) {
                continue;
            }
            executor.execute(() -> post(endpoint, event));
        }
    }

    private void post(DataGeneratorProperties.AiQuotaWebhookEndpoint endpoint, AiQuotaAlertEvent event) {
        try {
            String body = TemplateJsonCodec.write(toPayload(event));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint.getUrl().trim()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (StringUtils.hasText(endpoint.getSecretHeaderName()) && endpoint.getSecretValue() != null) {
                builder.header(endpoint.getSecretHeaderName().trim(), endpoint.getSecretValue());
            }
            httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
        }
        catch (Exception ignored) {
            // Quota enforcement must not fail when a webhook endpoint is unreachable.
        }
    }

    private static Map<String, Object> toPayload(AiQuotaAlertEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event.eventType());
        payload.put("usageDate", event.usageDate());
        payload.put("scopeKey", event.scopeKey());
        payload.put("scopeType", event.scopeType());
        payload.put("dimension", event.dimension());
        payload.put("used", event.used());
        payload.put("max", event.max());
        if (event.percentUsed() != null) {
            payload.put("percentUsed", event.percentUsed());
        }
        return payload;
    }

    private static boolean accepts(DataGeneratorProperties.AiQuotaWebhookEndpoint endpoint, String eventType) {
        List<String> events = endpoint.getEvents();
        if (events == null || events.isEmpty()) {
            return true;
        }
        String normalized = eventType == null ? "" : eventType.trim().toUpperCase(Locale.ROOT);
        for (String configured : events) {
            if (configured != null && configured.trim().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private DataGeneratorProperties.AiRuntimeQuota quotaConfig() {
        DataGeneratorProperties.AiRuntime aiRuntime = properties == null
                ? new DataGeneratorProperties.AiRuntime()
                : properties.getAiRuntime();
        return aiRuntime == null || aiRuntime.getQuota() == null
                ? new DataGeneratorProperties.AiRuntimeQuota()
                : aiRuntime.getQuota();
    }
}
