/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;

/**
 * Platform-level AI usage rollup from persisted V2 run reports.
 *
 * @param jobsWithAiCalls    successful jobs that recorded at least one AI call
 * @param totalCalls         total AI calls across those jobs
 * @param promptTokens       summed prompt tokens
 * @param completionTokens   summed completion tokens
 * @param totalLatencyMs     summed wall-clock latency
 * @param byProvider         per-provider breakdown sorted by call volume
 * @author Gensokyo
 * @since 2026-06-12
 */
public record AiUsageSummaryDto(
        long jobsWithAiCalls,
        long totalCalls,
        long promptTokens,
        long completionTokens,
        long totalLatencyMs,
        List<AiProviderUsageDto> byProvider) {
}
