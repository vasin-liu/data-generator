/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Per-call diagnostics for remote AI providers collected during a Template V2 run.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
public final class AiCallMetric {

    private final String sourceName;
    private final String providerType;
    private final String model;
    private final long promptTokens;
    private final long completionTokens;
    private final long latencyMs;
    private final int attempts;
    private final String responseSample;

    private AiCallMetric(
            String sourceName,
            String providerType,
            String model,
            long promptTokens,
            long completionTokens,
            long latencyMs,
            int attempts,
            String responseSample) {
        this.sourceName = sourceName;
        this.providerType = providerType;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.latencyMs = latencyMs;
        this.attempts = attempts;
        this.responseSample = responseSample;
    }

    /**
     * Builds a remote-provider call metric.
     *
     * @param providerType    provider identifier
     * @param model           resolved model name when known
     * @param promptTokens    prompt token count when reported by the provider
     * @param completionTokens completion token count when reported by the provider
     * @param latencyMs       wall-clock latency for the successful call
     * @param attempts        number of attempts including retries
     * @param responseSample  truncated response text sample
     * @return call metric without a bound source name
     */
    public static AiCallMetric remote(
            String providerType,
            String model,
            long promptTokens,
            long completionTokens,
            long latencyMs,
            int attempts,
            String responseSample) {
        return new AiCallMetric(
                null,
                providerType,
                model,
                promptTokens,
                completionTokens,
                latencyMs,
                attempts,
                truncateSample(responseSample));
    }

    /**
     * Returns a copy with the logical source name attached.
     *
     * @param sourceName template source key
     * @return metric bound to the source
     */
    public AiCallMetric withSourceName(String sourceName) {
        return new AiCallMetric(
                sourceName,
                providerType,
                model,
                promptTokens,
                completionTokens,
                latencyMs,
                attempts,
                responseSample);
    }

    /**
     * Logical source name when bound during materialization.
     *
     * @return source key, or {@code null} before binding
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Provider type for this call.
     *
     * @return provider identifier
     */
    public String getProviderType() {
        return providerType;
    }

    /**
     * Resolved model name when reported by the provider.
     *
     * @return model name, or {@code null} when unknown
     */
    public String getModel() {
        return model;
    }

    /**
     * Prompt token count when reported by the provider.
     *
     * @return prompt tokens, or {@code 0} when unavailable
     */
    public long getPromptTokens() {
        return promptTokens;
    }

    /**
     * Completion token count when reported by the provider.
     *
     * @return completion tokens, or {@code 0} when unavailable
     */
    public long getCompletionTokens() {
        return completionTokens;
    }

    /**
     * Wall-clock latency for the successful call in milliseconds.
     *
     * @return latency in milliseconds
     */
    public long getLatencyMs() {
        return latencyMs;
    }

    /**
     * Number of attempts including retries.
     *
     * @return attempt count
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Truncated response text sample for operator diagnostics.
     *
     * @return response sample, or {@code null} when empty
     */
    public String getResponseSample() {
        return responseSample;
    }

    private static String truncateSample(String sample) {
        if (sample == null || sample.isBlank()) {
            return null;
        }
        if (sample.length() <= 200) {
            return sample;
        }
        return sample.substring(0, 200);
    }
}
