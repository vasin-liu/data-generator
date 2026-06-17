/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

/**
 * Resolved quota limits for one scoped bucket (provider or template).
 *
 * @param scopeKey         canonical bucket key
 * @param scopeType        {@code PROVIDER} or {@code TEMPLATE}
 * @param scopeLabel       operator-facing label
 * @param maxCallsPerDay   call cap (0 = unlimited)
 * @param maxTokensPerDay  token cap (0 = unlimited)
 * @param maxCostUsdPerDay USD cap (0 = unlimited)
 * @author Gensokyo
 * @since 2026-06-12
 */
public record AiQuotaScopedPolicy(
        String scopeKey,
        String scopeType,
        String scopeLabel,
        long maxCallsPerDay,
        long maxTokensPerDay,
        double maxCostUsdPerDay) {

    /**
     * @return {@code true} when at least one limit is configured
     */
    public boolean active() {
        return maxCallsPerDay > 0L || maxTokensPerDay > 0L || maxCostUsdPerDay > 0.0D;
    }
}
