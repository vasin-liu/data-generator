/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

/**
 * Quota warn or exceed signal for audit logs and outbound webhooks.
 *
 * @param eventType    {@code WARN} or {@code EXCEEDED}
 * @param usageDate    UTC day key
 * @param scopeKey     canonical quota bucket key
 * @param scopeType    scope category (e.g. {@code PLATFORM}, {@code TENANT})
 * @param dimension    {@code CALLS}, {@code TOKENS}, or {@code COST}
 * @param used         current usage for the dimension
 * @param max          configured cap for the dimension
 * @param percentUsed  usage percent when {@code eventType} is {@code WARN}, else {@code null}
 * @author Gensokyo
 * @since 2026-06-17
 */
public record AiQuotaAlertEvent(
        String eventType,
        String usageDate,
        String scopeKey,
        String scopeType,
        String dimension,
        double used,
        double max,
        Double percentUsed) {
}
