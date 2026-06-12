/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

/**
 * Live Ollama integration for the Template V2 AI runtime bridge.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
class OllamaAiRuntimeBridgeLiveIT {

    /**
     * Calls a local Ollama broker when reachable and records traced call metrics.
     */
    @Test
    void generateTracedAgainstLocalOllamaBroker() {
        assumeOllamaAvailable();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.refresh();
            OllamaAiRuntimeBridge bridge = new OllamaAiRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory());

            AiSourceVO source = new AiSourceVO();
            source.setApi("http://localhost:11434");
            AiProviderVO provider = new AiProviderVO();
            provider.setType("OLLAMA");
            provider.setOptions(Map.of("model", "qwen2.5:0.5b"));
            source.setProvider(provider);
            source.setPrompt("Reply with exactly: ok");

            AiGenerateResult result = bridge.generateTraced(source);

            org.junit.jupiter.api.Assertions.assertNotNull(result.payload());
            org.junit.jupiter.api.Assertions.assertFalse(String.valueOf(result.payload()).isBlank());
            org.junit.jupiter.api.Assertions.assertNotNull(result.metric());
            org.junit.jupiter.api.Assertions.assertEquals("OLLAMA", result.metric().getProviderType());
            org.junit.jupiter.api.Assertions.assertTrue(result.metric().getLatencyMs() >= 0L);
        }
    }

    private static void assumeOllamaAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 11434), 1000);
        }
        catch (Exception ex) {
            Assumptions.assumeTrue(false, "Ollama is not available on localhost:11434");
        }
    }
}
