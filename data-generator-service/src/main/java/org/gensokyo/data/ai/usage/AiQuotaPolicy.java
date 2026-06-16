/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.config.DataGeneratorProperties;

/**
 * Resolved platform AI daily quota limits (0 = unlimited per dimension).
 *
 * @param enabled            whether quota enforcement is active
 * @param maxCallsPerDay     max AI calls per UTC day
 * @param maxTokensPerDay    max prompt+completion tokens per UTC day
 * @param maxCostUsdPerDay   max estimated USD spend per UTC day
 * @author Gensokyo
 * @since 2026-06-12
 */
public record AiQuotaPolicy(
        boolean enabled,
        long maxCallsPerDay,
        long maxTokensPerDay,
        double maxCostUsdPerDay) {

    /**
     * @param quota configured quota block
     * @return resolved policy with non-negative limits
     */
    public static AiQuotaPolicy resolve(DataGeneratorProperties.AiRuntimeQuota quota) {
        if (quota == null || !quota.isEnabled()) {
            return new AiQuotaPolicy(false, 0L, 0L, 0.0D);
        }
        long maxCalls = quota.getMaxCallsPerDay() == null ? 0L : Math.max(0L, quota.getMaxCallsPerDay());
        long maxTokens = quota.getMaxTokensPerDay() == null ? 0L : Math.max(0L, quota.getMaxTokensPerDay());
        double maxCost = quota.getMaxCostUsdPerDay() == null ? 0.0D : Math.max(0.0D, quota.getMaxCostUsdPerDay());
        boolean active = maxCalls > 0L || maxTokens > 0L || maxCost > 0.0D;
        return new AiQuotaPolicy(active, maxCalls, maxTokens, maxCost);
    }
}
