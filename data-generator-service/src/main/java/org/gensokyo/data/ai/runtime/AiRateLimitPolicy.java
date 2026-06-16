/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.config.DataGeneratorProperties;

import java.util.Map;

/**
 * Resolved AI call throttle policy from provider options with platform defaults.
 *
 * @param minIntervalMs      minimum gap between calls for the same limiter key (0 = disabled)
 * @param requestsPerMinute  max calls per rolling minute (0 = disabled)
 * @author Gensokyo
 * @since 2026-06-12
 */
record AiRateLimitPolicy(long minIntervalMs, int requestsPerMinute) {

    private static final long WINDOW_MS = 60_000L;

    /**
     * @return whether any throttle rule is active
     */
    boolean enabled() {
        return minIntervalMs > 0L || requestsPerMinute > 0;
    }

    /**
     * Merges per-provider options with {@link DataGeneratorProperties.AiRuntime} defaults.
     *
     * @param providerOptions provider option map
     * @param defaults        platform defaults
     * @return resolved policy
     */
    static AiRateLimitPolicy resolve(Map<String, Object> providerOptions, DataGeneratorProperties.AiRuntime defaults) {
        DataGeneratorProperties.AiRuntime safeDefaults = defaults == null ? new DataGeneratorProperties.AiRuntime() : defaults;
        long minIntervalMs = longOption(providerOptions, "minIntervalMs", safeDefaults.getDefaultMinIntervalMs());
        int requestsPerMinute = intOption(providerOptions, "requestsPerMinute", safeDefaults.getDefaultRequestsPerMinute());
        return new AiRateLimitPolicy(minIntervalMs, requestsPerMinute);
    }

    long windowMs() {
        return WINDOW_MS;
    }

    private static long longOption(Map<String, Object> options, String key, Long defaultValue) {
        long fallback = defaultValue == null ? 0L : Math.max(0L, defaultValue);
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return fallback;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return Math.max(0L, number.longValue());
        }
        try {
            return Math.max(0L, Long.parseLong(String.valueOf(value).trim()));
        }
        catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int intOption(Map<String, Object> options, String key, Integer defaultValue) {
        int fallback = defaultValue == null ? 0 : Math.max(0, defaultValue);
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return fallback;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(value).trim()));
        }
        catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
