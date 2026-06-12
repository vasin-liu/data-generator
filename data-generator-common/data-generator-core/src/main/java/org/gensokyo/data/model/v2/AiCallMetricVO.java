/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import java.io.Serializable;

/**
 * AI provider call diagnostics exposed in Template V2 run reports.
 *
 * @param sourceName        logical source key
 * @param providerType      provider identifier
 * @param model             resolved model name when known
 * @param promptTokens      prompt token count when reported
 * @param completionTokens  completion token count when reported
 * @param latencyMs         wall-clock latency for the successful call
 * @param attempts          number of attempts including retries
 * @param responseSample    truncated response text sample
 * @author Gensokyo
 * @since 2026-06-11
 */
public record AiCallMetricVO(
        String sourceName,
        String providerType,
        String model,
        Long promptTokens,
        Long completionTokens,
        Long latencyMs,
        Integer attempts,
        String responseSample) implements Serializable {
}
