/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.calcite.runtime.AiCallMetric;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OpenAI-compatible chat-completions bridge for {@code OPENAI} and {@code AZURE_OPENAI} providers.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public class OpenAiCompatibleRuntimeBridge implements AiRuntimeBridge {

    private static final String OPENAI = "OPENAI";
    private static final String AZURE_OPENAI = "AZURE_OPENAI";
    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com";

    private final AiRuntimeBridgeSupport support;

    public OpenAiCompatibleRuntimeBridge(ApplicationContext applicationContext,
                                           AutowireCapableBeanFactory beanFactory) {
        this.support = new AiRuntimeBridgeSupport(applicationContext, beanFactory);
    }

    @Override
    public boolean supports(AiProviderVO provider) {
        if (provider == null || !StringUtils.hasText(provider.getType())) {
            return false;
        }
        String type = provider.getType().trim().toUpperCase(Locale.ROOT);
        return OPENAI.equals(type) || AZURE_OPENAI.equals(type);
    }

    @Override
    public Object generate(AiSourceVO source) {
        return generateTraced(source).payload();
    }

    @Override
    public AiGenerateResult generateTraced(AiSourceVO source) {
        Assert.notNull(source, "AI source must not be null");
        AiProviderVO provider = source.getProvider();
        if (!supports(provider)) {
            String providerType = provider == null ? "null" : provider.getType();
            throw new UnsupportedOperationException("AI provider type [" + providerType + "] is not supported by OpenAI runtime bridge");
        }

        String providerType = provider.getType().trim().toUpperCase(Locale.ROOT);
        Map<String, Object> providerOptions = provider.getOptions() == null ? Map.of() : provider.getOptions();
        String model = requireModel(providerOptions);
        String apiKey = requireApiKey(providerOptions);
        String endpoint = resolveEndpoint(source, providerType, providerOptions);

        RemoteAiContentCall call = support.executeWithRetry(
                providerOptions,
                () -> invokeChatCompletions(endpoint, apiKey, model, source.getPrompt()));
        Object payload = support.parse(source, call.content());
        AiCallMetric metric = AiCallMetric.remote(
                providerType,
                model,
                call.promptTokens(),
                call.completionTokens(),
                call.latencyMs(),
                call.attempts(),
                call.content());
        return new AiGenerateResult(payload, metric);
    }

    /**
     * Calls the chat-completions endpoint. Subclasses may override for tests.
     *
     * @param endpoint absolute chat-completions URL
     * @param apiKey   bearer token
     * @param model    model or deployment name
     * @param prompt   user prompt text
     * @return provider response payload
     */
    protected RemoteAiContentCall invokeChatCompletions(String endpoint, String apiKey, String model, String prompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt == null ? "" : prompt)));
        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient(apiKey)
                .post()
                .uri(endpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (response == null) {
            return new RemoteAiContentCall("", 0L, 0L, 1, 0L);
        }
        String content = extractContent(response);
        long promptTokens = extractUsageField(response, "prompt_tokens");
        long completionTokens = extractUsageField(response, "completion_tokens");
        return new RemoteAiContentCall(content, promptTokens, completionTokens, 1, 0L);
    }

    @SuppressWarnings("unchecked")
    private static String extractContent(Map<String, Object> response) {
        Object choicesNode = response.get("choices");
        if (!(choicesNode instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object first = choices.getFirst();
        if (!(first instanceof Map<?, ?> choice)) {
            return "";
        }
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return "";
        }
        Object content = messageMap.get("content");
        return content == null ? "" : String.valueOf(content);
    }

    @SuppressWarnings("unchecked")
    private static long extractUsageField(Map<String, Object> response, String field) {
        Object usageNode = response.get("usage");
        if (!(usageNode instanceof Map<?, ?> usage)) {
            return 0L;
        }
        Object value = usage.get(field);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static WebClient webClient(String apiKey) {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static String resolveEndpoint(AiSourceVO source, String providerType, Map<String, Object> options) {
        if (AZURE_OPENAI.equals(providerType)) {
            if (!StringUtils.hasText(source.getApi())) {
                throw new IllegalArgumentException("AZURE_OPENAI provider requires source.api deployment URL");
            }
            return source.getApi().trim();
        }
        String baseUrl = StringUtils.hasText(source.getApi()) ? source.getApi().trim() : DEFAULT_OPENAI_BASE_URL;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/v1/chat/completions";
    }

    private static String requireApiKey(Map<String, Object> options) {
        String apiKey = AiRuntimeBridgeSupport.stringOption(options, "apiKey");
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("OpenAI provider requires provider.options.apiKey");
        }
        return apiKey;
    }

    private static String requireModel(Map<String, Object> options) {
        String model = AiRuntimeBridgeSupport.stringOption(options, "model");
        if (!StringUtils.hasText(model)) {
            throw new IllegalArgumentException("OpenAI provider requires provider.options.model");
        }
        return model;
    }
}
