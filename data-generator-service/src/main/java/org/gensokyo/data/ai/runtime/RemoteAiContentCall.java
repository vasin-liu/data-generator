/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

/**
 * Remote AI provider response payload and token counters before parser materialization.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public record RemoteAiContentCall(
        String content,
        long promptTokens,
        long completionTokens,
        int attempts,
        long latencyMs) {

    /**
     * Returns a copy with retry attempt count and measured latency applied.
     *
     * @param attempts  number of attempts including retries
     * @param latencyMs wall-clock latency for the successful attempt
     * @return updated call metadata
     */
    public RemoteAiContentCall withAttemptsAndLatency(int attempts, long latencyMs) {
        return new RemoteAiContentCall(content, promptTokens, completionTokens, attempts, latencyMs);
    }
}
