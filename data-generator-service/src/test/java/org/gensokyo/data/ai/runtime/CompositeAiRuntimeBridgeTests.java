/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.calcite.runtime.AiCallMetric;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link CompositeAiRuntimeBridge}.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
class CompositeAiRuntimeBridgeTests {

    @Test
    void routesToFirstSupportingDelegate() {
        RecordingBridge ollama = new RecordingBridge("OLLAMA");
        RecordingBridge openAi = new RecordingBridge("OPENAI");
        CompositeAiRuntimeBridge bridge = new CompositeAiRuntimeBridge(List.of(ollama, openAi));

        AiSourceVO source = source("OPENAI", "openai payload");

        Object output = bridge.generate(source);

        Assertions.assertEquals("openai payload", output);
        Assertions.assertFalse(ollama.invoked);
        Assertions.assertTrue(openAi.invoked);
    }

    @Test
    void rejectsUnknownProviderWithConfiguredBridgeList() {
        CompositeAiRuntimeBridge bridge = new CompositeAiRuntimeBridge(List.of(
                new RecordingBridge("OLLAMA"),
                new RecordingBridge("OPENAI")));

        UnsupportedOperationException failure = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> bridge.generate(source("ANTHROPIC", "ignored")));

        Assertions.assertTrue(failure.getMessage().contains("ANTHROPIC"));
        Assertions.assertTrue(failure.getMessage().contains("RecordingBridge"));
    }

    private static AiSourceVO source(String providerType, String payload) {
        AiProviderVO provider = new AiProviderVO();
        provider.setType(providerType);
        AiSourceVO source = new AiSourceVO();
        source.setProvider(provider);
        return source;
    }

    private static final class RecordingBridge implements AiRuntimeBridge {
        private final String providerType;
        private boolean invoked;

        private RecordingBridge(String providerType) {
            this.providerType = providerType;
        }

        @Override
        public boolean supports(AiProviderVO provider) {
            return provider != null && providerType.equalsIgnoreCase(provider.getType());
        }

        @Override
        public Object generate(AiSourceVO source) {
            return generateTraced(source).payload();
        }

        @Override
        public AiGenerateResult generateTraced(AiSourceVO source) {
            invoked = true;
            return new AiGenerateResult(
                    providerType.toLowerCase() + " payload",
                    AiCallMetric.remote(providerType, "demo", 1L, 2L, 3L, 1, "demo"));
        }
    }
}
