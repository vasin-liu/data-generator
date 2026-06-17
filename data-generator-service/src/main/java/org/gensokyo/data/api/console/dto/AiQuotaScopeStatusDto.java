/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Scoped AI daily quota limits and current UTC-day consumption.
 *
 * @param scopeKey               canonical bucket key
 * @param scopeType              {@code PROVIDER} or {@code TEMPLATE}
 * @param scopeLabel             operator-facing label
 * @param maxCallsPerDay         configured call cap (0 = unlimited)
 * @param maxTokensPerDay        configured token cap (0 = unlimited)
 * @param maxCostUsdPerDay       configured USD cap (0 = unlimited)
 * @param usedCalls              calls recorded today
 * @param usedPromptTokens       prompt tokens recorded today
 * @param usedCompletionTokens   completion tokens recorded today
 * @param usedCostUsd            estimated USD recorded today
 * @param remainingCalls         calls left today, or {@code null} when unlimited
 * @param remainingTokens        tokens left today, or {@code null} when unlimited
 * @param remainingCostUsd       USD left today, or {@code null} when unlimited
 * @author Gensokyo
 * @since 2026-06-12
 */
public record AiQuotaScopeStatusDto(
        String scopeKey,
        String scopeType,
        String scopeLabel,
        long maxCallsPerDay,
        long maxTokensPerDay,
        double maxCostUsdPerDay,
        long usedCalls,
        long usedPromptTokens,
        long usedCompletionTokens,
        double usedCostUsd,
        Long remainingCalls,
        Long remainingTokens,
        Double remainingCostUsd) {
}
