package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.runtime.AiCallMetric;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;

public interface AiRuntimeBridge {
    boolean supports(AiProviderVO provider);

    Object generate(AiSourceVO source);

    /**
     * Generates provider output and returns optional call diagnostics for run reports.
     *
     * @param source AI source definition
     * @return payload and optional traced metrics
     */
    default AiGenerateResult generateTraced(AiSourceVO source) {
        long startNanos = System.nanoTime();
        Object payload = generate(source);
        long latencyMs = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        String providerType = source.getProvider() == null ? null : source.getProvider().getType();
        return new AiGenerateResult(
                payload,
                AiCallMetric.remote(providerType, null, 0L, 0L, latencyMs, 1, null));
    }
}
