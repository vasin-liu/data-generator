package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.ai.ollama.api.OllamaOptions;
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

class OllamaAiRuntimeBridgeTests {

    @Test
    void supportsOllamaProviderType() {
        try (AnnotationConfigApplicationContext context = context()) {
            OllamaAiRuntimeBridge bridge = new OllamaAiRuntimeBridge(context, context.getAutowireCapableBeanFactory());
            AiProviderVO provider = new AiProviderVO();
            provider.setType("OLLAMA");

            Assertions.assertTrue(bridge.supports(provider));
        }
    }

    @Test
    void parsesContentWithNamedOutputParserBean() {
        try (AnnotationConfigApplicationContext context = context()) {
            RecordingOllamaAiRuntimeBridge bridge = new RecordingOllamaAiRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    "alpha,beta,gamma"
            );
            AiSourceVO source = source("OLLAMA", Map.of("model", "qwen2", "temperature", 0.1f));
            source.setPrompt("generate three values");
            source.setParser("namedListParser");

            Object output = bridge.generate(source);

            Assertions.assertInstanceOf(List.class, output);
            Assertions.assertEquals(List.of("alpha", "beta", "gamma"), output);
            Assertions.assertEquals("generate three values", bridge.lastPrompt);
            Assertions.assertEquals("qwen2", bridge.lastOptions.getModel());
            Assertions.assertEquals(0.1f, bridge.lastOptions.getTemperature());
        }
    }

    @Test
    void resolvesParserByClassNameWhenBeanNameIsAbsent() {
        try (AnnotationConfigApplicationContext context = context()) {
            RecordingOllamaAiRuntimeBridge bridge = new RecordingOllamaAiRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    "foo,bar"
            );
            AiSourceVO source = source("OLLAMA", Map.of("model", "qwen2"));
            source.setParser(ListOutputParser.class.getName());

            Object output = bridge.generate(source);

            Assertions.assertEquals(List.of("foo", "bar"), output);
        }
    }

    @Test
    void fallsBackToRawContentWhenParserIsMissing() {
        try (AnnotationConfigApplicationContext context = context()) {
            RecordingOllamaAiRuntimeBridge bridge = new RecordingOllamaAiRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    "{\"name\":\"alice\"}"
            );
            AiSourceVO source = source("OLLAMA", Map.of());

            Object output = bridge.generate(source);

            Assertions.assertEquals("{\"name\":\"alice\"}", output);
            Assertions.assertEquals(OllamaOptions.DEFAULT_MODEL, bridge.lastOptions.getModel());
        }
    }

    @Test
    void generateTracedCapturesModelLatencyAndAttempts() {
        try (AnnotationConfigApplicationContext context = context()) {
            RecordingOllamaAiRuntimeBridge bridge = new RecordingOllamaAiRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    "alpha,beta",
                    11L,
                    7L);
            AiSourceVO source = source("OLLAMA", Map.of("model", "qwen2", "maxRetries", 2));
            source.setParser("namedListParser");

            AiGenerateResult result = bridge.generateTraced(source);

            Assertions.assertEquals(List.of("alpha", "beta"), result.payload());
            Assertions.assertNotNull(result.metric());
            Assertions.assertEquals("OLLAMA", result.metric().getProviderType());
            Assertions.assertEquals("qwen2", result.metric().getModel());
            Assertions.assertEquals(11L, result.metric().getPromptTokens());
            Assertions.assertEquals(7L, result.metric().getCompletionTokens());
            Assertions.assertEquals(1, result.metric().getAttempts());
            Assertions.assertTrue(result.metric().getLatencyMs() >= 0L);
        }
    }

    @Test
    void retriesTransientGenerateFailuresBeforeGivingUp() {
        try (AnnotationConfigApplicationContext context = context()) {
            FlakyOllamaAiRuntimeBridge bridge = new FlakyOllamaAiRuntimeBridge(
                    context,
                    context.getAutowireCapableBeanFactory(),
                    2,
                    "alpha,beta"
            );
            AiSourceVO source = source("OLLAMA", Map.of("maxRetries", 3, "retryBackoffMs", 1));

            Object output = bridge.generate(source);

            Assertions.assertEquals("alpha,beta", output);
            Assertions.assertEquals(2, bridge.attempts);
        }
    }

    @Test
    void rejectsUnsupportedProviderType() {
        try (AnnotationConfigApplicationContext context = context()) {
            OllamaAiRuntimeBridge bridge = new OllamaAiRuntimeBridge(context, context.getAutowireCapableBeanFactory());
            AiSourceVO source = source("OPENAI", Map.of());

            UnsupportedOperationException failure = Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> bridge.generate(source)
            );

            Assertions.assertTrue(failure.getMessage().contains("OPENAI"));
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
        source.setApi("http://localhost:11434");
        source.setProvider(provider);
        return source;
    }

    private static final class FlakyOllamaAiRuntimeBridge extends OllamaAiRuntimeBridge {
        private final int failUntilAttempt;
        private final String content;
        private int attempts;

        private FlakyOllamaAiRuntimeBridge(AnnotationConfigApplicationContext applicationContext,
                                           AutowireCapableBeanFactory beanFactory,
                                           int failUntilAttempt,
                                           String content) {
            super(applicationContext, beanFactory);
            this.failUntilAttempt = failUntilAttempt;
            this.content = content;
        }

        @Override
        protected RemoteAiContentCall generateContentCall(AiSourceVO source, OllamaOptions options) {
            attempts++;
            if (attempts < failUntilAttempt) {
                throw new IllegalStateException("transient ai failure");
            }
            return new RemoteAiContentCall(content, 0L, 0L, 1, 0L);
        }
    }

    private static final class RecordingOllamaAiRuntimeBridge extends OllamaAiRuntimeBridge {
        private final String content;
        private final long promptTokens;
        private final long completionTokens;
        private String lastPrompt;
        private OllamaOptions lastOptions;

        private RecordingOllamaAiRuntimeBridge(AnnotationConfigApplicationContext applicationContext,
                                               AutowireCapableBeanFactory beanFactory,
                                               String content) {
            this(applicationContext, beanFactory, content, 0L, 0L);
        }

        private RecordingOllamaAiRuntimeBridge(AnnotationConfigApplicationContext applicationContext,
                                               AutowireCapableBeanFactory beanFactory,
                                               String content,
                                               long promptTokens,
                                               long completionTokens) {
            super(applicationContext, beanFactory);
            this.content = content;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }

        @Override
        protected RemoteAiContentCall generateContentCall(AiSourceVO source, OllamaOptions options) {
            this.lastPrompt = source.getPrompt();
            this.lastOptions = options;
            return new RemoteAiContentCall(content, promptTokens, completionTokens, 1, 0L);
        }
    }
}
