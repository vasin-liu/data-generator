/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Platform AI daily quota limits and current UTC-day consumption.
 *
 * @param enabled                 whether quota enforcement is configured
 * @param usageDate               UTC day key ({@code yyyy-MM-dd})
 * @param maxCallsPerDay          configured call cap (0 = unlimited)
 * @param maxTokensPerDay         configured token cap (0 = unlimited)
 * @param maxCostUsdPerDay        configured USD cap (0 = unlimited)
 * @param usedCalls               calls recorded today
 * @param usedPromptTokens        prompt tokens recorded today
 * @param usedCompletionTokens    completion tokens recorded today
 * @param usedCostUsd             estimated USD recorded today
 * @param remainingCalls          calls left today, or {@code null} when unlimited
 * @param remainingTokens         tokens left today, or {@code null} when unlimited
 * @param remainingCostUsd        USD left today, or {@code null} when unlimited
 * @param alertsEnabled           whether quota warn/exceed audit hooks are active
 * @param warnAtPercent           warn threshold percent (0 = disabled)
 * @param scopes                  configured provider/template scoped quotas
 * @author Gensokyo
 * @since 2026-06-12
 */
public record AiQuotaStatusDto(
        boolean enabled,
        String usageDate,
        long maxCallsPerDay,
        long maxTokensPerDay,
        double maxCostUsdPerDay,
        long usedCalls,
        long usedPromptTokens,
        long usedCompletionTokens,
        double usedCostUsd,
        Long remainingCalls,
        Long remainingTokens,
        Double remainingCostUsd,
        boolean alertsEnabled,
        int warnAtPercent,
        java.util.List<AiQuotaScopeStatusDto> scopes) {
}
