/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.ai.parser.ListOutputParser;
import org.gensokyo.data.ai.parser.OutputParser;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link OpenAiCompatibleRuntimeBridge}.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
class OpenAiCompatibleRuntimeBridgeTests {

    @Test
    void supportsOpenAiAndAzureProviderTypes() {
        try (AnnotationConfigApplicationContext context = context()) {
            OpenAiCompatibleRuntimeBridge bridge = new OpenAiCompatibleRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory());

            AiProviderVO openAi = new AiProviderVO();
            openAi.setType("OPENAI");
            AiProviderVO azure = new AiProviderVO();
            azure.setType("AZURE_OPENAI");

            Assertions.assertTrue(bridge.supports(openAi));
            Assertions.assertTrue(bridge.supports(azure));
        }
    }

    @Test
    void generateTracedParsesListOutputAndRecordsUsage() {
        try (AnnotationConfigApplicationContext context = context()) {
            RecordingOpenAiBridge bridge = new RecordingOpenAiBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    "alpha,beta");
            AiSourceVO source = source("OPENAI", Map.of("model", "gpt-4o-mini", "apiKey", "test-key"));
            source.setPrompt("generate values");
            source.setParser(ListOutputParser.class.getName());

            AiGenerateResult result = bridge.generateTraced(source);

            Assertions.assertEquals(List.of("alpha", "beta"), result.payload());
            Assertions.assertEquals("OPENAI", result.metric().getProviderType());
            Assertions.assertEquals("gpt-4o-mini", result.metric().getModel());
            Assertions.assertEquals(12L, result.metric().getPromptTokens());
            Assertions.assertEquals(4L, result.metric().getCompletionTokens());
            Assertions.assertEquals("https://api.openai.com/v1/chat/completions", bridge.lastEndpoint);
        }
    }

    @Test
    void requiresApiKeyForOpenAiProvider() {
        try (AnnotationConfigApplicationContext context = context()) {
            OpenAiCompatibleRuntimeBridge bridge = new OpenAiCompatibleRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory());
            AiSourceVO source = source("OPENAI", Map.of("model", "gpt-4o-mini"));

            IllegalArgumentException failure = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> bridge.generate(source));

            Assertions.assertTrue(failure.getMessage().contains("apiKey"));
        }
    }

    private AnnotationConfigApplicationContext context() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean("namedListParser", OutputParser.class,
                () -> new ListOutputParser(new DefaultConversionService()));
        context.refresh();
        return context;
    }

    private AiSourceVO source(String providerType, Map<String, Object> options) {
        AiProviderVO provider = new AiProviderVO();
        provider.setType(providerType);
        provider.setOptions(options);
        AiSourceVO source = new AiSourceVO();
        source.setProvider(provider);
        return source;
    }

    private static final class RecordingOpenAiBridge extends OpenAiCompatibleRuntimeBridge {
        private final String content;
        private String lastEndpoint;

        private RecordingOpenAiBridge(AnnotationConfigApplicationContext applicationContext,
                                      AutowireCapableBeanFactory beanFactory,
                                      String content) {
            super(applicationContext, beanFactory);
            this.content = content;
        }

        @Override
        protected RemoteAiContentCall invokeChatCompletions(String endpoint, String apiKey, String model, String prompt) {
            this.lastEndpoint = endpoint;
            return new RemoteAiContentCall(content, 12L, 4L, 1, 0L);
        }
    }
}
