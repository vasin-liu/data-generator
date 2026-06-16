/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.ai.parser.ListOutputParser;
import org.gensokyo.data.ai.parser.OutputParser;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.gensokyo.data.secret.SecretResolver;
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

    private static final SecretResolver PLAINTEXT_RESOLVER = new SecretResolver() {
        @Override
        public String resolveRequired(String secretRef) {
            throw new UnsupportedOperationException(secretRef);
        }
    };
    private static final AiRateLimiter RATE_LIMITER = new AiRateLimiter();
    private static final DataGeneratorProperties PROPERTIES = new DataGeneratorProperties();

    @Test
    void supportsOpenAiAndAzureProviderTypes() {
        try (AnnotationConfigApplicationContext context = context()) {
            OpenAiCompatibleRuntimeBridge bridge = new OpenAiCompatibleRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    PLAINTEXT_RESOLVER,
                    RATE_LIMITER,
                    PROPERTIES);

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
                    PLAINTEXT_RESOLVER,
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
    void resolvesApiKeyFromSecretRef() {
        try (AnnotationConfigApplicationContext context = context()) {
            SecretResolver resolver = new SecretResolver() {
                @Override
                public String resolveRequired(String secretRef) {
                    Assertions.assertEquals("secrets/ai/openai", secretRef);
                    return "vault-key";
                }
            };
            RecordingOpenAiBridge bridge = new RecordingOpenAiBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    resolver,
                    "one,two");
            AiSourceVO source = source("OPENAI", Map.of(
                    "model", "gpt-4o-mini",
                    "apiKeySecretRef", "secrets/ai/openai"));
            source.setPrompt("generate values");
            source.setParser(ListOutputParser.class.getName());

            bridge.generateTraced(source);

            Assertions.assertEquals("vault-key", bridge.lastApiKey);
        }
    }

    @Test
    void requiresApiKeyForOpenAiProvider() {
        try (AnnotationConfigApplicationContext context = context()) {
            OpenAiCompatibleRuntimeBridge bridge = new OpenAiCompatibleRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    PLAINTEXT_RESOLVER,
                    RATE_LIMITER,
                    PROPERTIES);
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
        private String lastApiKey;

        private RecordingOpenAiBridge(AnnotationConfigApplicationContext applicationContext,
                                      AutowireCapableBeanFactory beanFactory,
                                      SecretResolver secretResolver,
                                      String content) {
            super(applicationContext, beanFactory, secretResolver, RATE_LIMITER, PROPERTIES);
            this.content = content;
        }

        @Override
        protected RemoteAiContentCall invokeChatCompletions(String endpoint, String apiKey, String model, String prompt) {
            this.lastEndpoint = endpoint;
            this.lastApiKey = apiKey;
            return new RemoteAiContentCall(content, 12L, 4L, 1, 0L);
        }
    }
}
