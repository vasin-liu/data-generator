/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Aggregated AI token usage for a single provider type across successful job runs.
 *
 * @param providerType       provider identifier (e.g. {@code OLLAMA})
 * @param calls              number of AI calls
 * @param promptTokens       summed prompt tokens
 * @param completionTokens   summed completion tokens
 * @param latencyMs          summed wall-clock latency
 * @author Gensokyo
 * @since 2026-06-12
 */
public record AiProviderUsageDto(
        String providerType,
        long calls,
        long promptTokens,
        long completionTokens,
        long latencyMs) {
}
