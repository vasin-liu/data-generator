package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.ai.usage.AiQuotaService;
import org.gensokyo.data.ai.chat.ChatResponse;
import org.gensokyo.data.ai.chat.Generation;
import org.gensokyo.data.ai.chat.metadata.Usage;
import org.gensokyo.data.ai.chat.prompt.Prompt;
import org.gensokyo.data.ai.model.ModelOptionsUtils;
import org.gensokyo.data.ai.ollama.OllamaChatClient;
import org.gensokyo.data.ai.ollama.api.OllamaApi;
import org.gensokyo.data.ai.ollama.api.OllamaOptions;
import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.calcite.runtime.AiCallMetric;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Map;

public class OllamaAiRuntimeBridge implements AiRuntimeBridge {
    private static final String OLLAMA = "OLLAMA";

    private final AiRuntimeBridgeSupport support;

    public OllamaAiRuntimeBridge(ApplicationContext applicationContext,
                                 AutowireCapableBeanFactory beanFactory,
                                 AiRateLimiter rateLimiter,
                                 AiQuotaService aiQuotaService,
                                 DataGeneratorProperties properties) {
        this.support = new AiRuntimeBridgeSupport(
                applicationContext,
                beanFactory,
                rateLimiter,
                aiQuotaService,
                properties == null ? null : properties.getAiRuntime());
    }

    @Override
    public boolean supports(AiProviderVO provider) {
        return provider != null
                && StringUtils.hasText(provider.getType())
                && OLLAMA.equalsIgnoreCase(provider.getType().trim());
    }

    @Override
    public Object generate(AiSourceVO source) {
        return generateTraced(source).payload();
    }

    @Override
    public AiGenerateResult generateTraced(AiSourceVO source) {
        Assert.notNull(source, "AI source must not be null");
        AiProviderVO provider = source.getProvider();
        if (!supports(provider)) {
            String providerType = provider == null ? "null" : provider.getType();
            throw new UnsupportedOperationException("AI provider type [" + providerType + "] is not supported by Ollama runtime bridge");
        }

        OllamaOptions options = resolveOptions(provider);
        Map<String, Object> providerOptions = provider.getOptions() == null ? Map.of() : provider.getOptions();
        String rateLimitKey = AiRuntimeBridgeSupport.rateLimitKey(source, OLLAMA, providerOptions);
        RemoteAiContentCall call = support.executeWithRetry(
                OLLAMA,
                options.getModel(),
                rateLimitKey,
                providerOptions,
                () -> generateContentCall(source, options));
        Object payload = support.parse(source, call.content());
        AiCallMetric metric = AiCallMetric.remote(
                OLLAMA,
                options.getModel(),
                call.promptTokens(),
                call.completionTokens(),
                call.latencyMs(),
                call.attempts(),
                call.content());
        return new AiGenerateResult(payload, metric);
    }

    protected RemoteAiContentCall generateContentCall(AiSourceVO source, OllamaOptions options) {
        Prompt prompt = new Prompt(source.getPrompt() == null ? "" : source.getPrompt());
        ChatResponse response = createClient(source, options).call(prompt);
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return new RemoteAiContentCall("", 0L, 0L, 1, 0L);
        }
        Generation generation = response.getResult();
        String content = generation.getOutput().getContent();
        TokenUsage usage = extractUsage(generation);
        return new RemoteAiContentCall(content, usage.promptTokens(), usage.completionTokens(), 1, 0L);
    }

    private static TokenUsage extractUsage(Generation generation) {
        Object metadata = generation.getMetadata().getContentFilterMetadata();
        if (metadata instanceof Usage usage) {
            long promptTokens = usage.getPromptTokens() == null ? 0L : usage.getPromptTokens();
            long completionTokens = usage.getGenerationTokens() == null ? 0L : usage.getGenerationTokens();
            return new TokenUsage(promptTokens, completionTokens);
        }
        return new TokenUsage(0L, 0L);
    }

    private record TokenUsage(long promptTokens, long completionTokens) {
    }

    protected OllamaChatClient createClient(AiSourceVO source, OllamaOptions options) {
        return new OllamaChatClient(createApi(source), options);
    }

    protected OllamaApi createApi(AiSourceVO source) {
        return StringUtils.hasText(source.getApi()) ? new OllamaApi(source.getApi()) : new OllamaApi();
    }

    private OllamaOptions resolveOptions(AiProviderVO provider) {
        Map<String, Object> optionMap = provider.getOptions() == null ? Map.of() : provider.getOptions();
        return ModelOptionsUtils.merge(optionMap, OllamaOptions.create().withModel(OllamaOptions.DEFAULT_MODEL), OllamaOptions.class);
    }
}
