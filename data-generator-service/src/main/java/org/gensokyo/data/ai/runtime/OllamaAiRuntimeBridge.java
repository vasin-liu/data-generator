package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.ai.chat.ChatResponse;
import org.gensokyo.data.ai.chat.Generation;
import org.gensokyo.data.ai.chat.metadata.Usage;
import org.gensokyo.data.ai.chat.prompt.Prompt;
import org.gensokyo.data.ai.model.ModelOptionsUtils;
import org.gensokyo.data.ai.ollama.OllamaChatClient;
import org.gensokyo.data.ai.ollama.api.OllamaApi;
import org.gensokyo.data.ai.ollama.api.OllamaOptions;
import org.gensokyo.data.ai.parser.OutputParser;
import org.gensokyo.data.calcite.AiRuntimeBridge;
import org.gensokyo.data.calcite.runtime.AiCallMetric;
import org.gensokyo.data.calcite.runtime.AiGenerateResult;
import org.gensokyo.data.model.v2.AiProviderVO;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Map;

public class OllamaAiRuntimeBridge implements AiRuntimeBridge {
    private static final String OLLAMA = "OLLAMA";

    private final ApplicationContext applicationContext;
    private final AutowireCapableBeanFactory beanFactory;
    private final DefaultConversionService fallbackConversionService = new DefaultConversionService();

    public OllamaAiRuntimeBridge(ApplicationContext applicationContext,
                                 AutowireCapableBeanFactory beanFactory) {
        this.applicationContext = applicationContext;
        this.beanFactory = beanFactory;
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
        ContentCall call = generateWithRetry(source, options, provider.getOptions());
        Object payload = parse(source, call.content());
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

    private ContentCall generateWithRetry(AiSourceVO source, OllamaOptions options, Map<String, Object> providerOptions) {
        int maxRetries = intOption(providerOptions, "maxRetries", 1);
        long retryBackoffMs = longOption(providerOptions, "retryBackoffMs", 0L);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                long startNanos = System.nanoTime();
                ContentCall call = generateContentCall(source, options);
                long latencyMs = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
                return call.withAttemptsAndLatency(attempt, latencyMs);
            }
            catch (RuntimeException ex) {
                lastFailure = ex;
                if (attempt >= maxRetries) {
                    break;
                }
                sleepQuietly(retryBackoffMs);
            }
        }
        throw lastFailure;
    }

    private static int intOption(Map<String, Object> options, String key, int defaultValue) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        try {
            return Math.max(1, Integer.parseInt(String.valueOf(value).trim()));
        }
        catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static long longOption(Map<String, Object> options, String key, long defaultValue) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return Math.max(0L, number.longValue());
        }
        try {
            return Math.max(0L, Long.parseLong(String.valueOf(value).trim()));
        }
        catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static void sleepQuietly(long retryBackoffMs) {
        if (retryBackoffMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMs);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI retry backoff interrupted", ex);
        }
    }

    protected ContentCall generateContentCall(AiSourceVO source, OllamaOptions options) {
        Prompt prompt = new Prompt(source.getPrompt() == null ? "" : source.getPrompt());
        ChatResponse response = createClient(source, options).call(prompt);
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return new ContentCall("", 0L, 0L, 1, 0L);
        }
        Generation generation = response.getResult();
        String content = generation.getOutput().getContent();
        TokenUsage usage = extractUsage(generation);
        return new ContentCall(content, usage.promptTokens(), usage.completionTokens(), 1, 0L);
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

    protected record ContentCall(
            String content,
            long promptTokens,
            long completionTokens,
            int attempts,
            long latencyMs) {

        ContentCall withAttemptsAndLatency(int attempts, long latencyMs) {
            return new ContentCall(content, promptTokens, completionTokens, attempts, latencyMs);
        }
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

    private Object parse(AiSourceVO source, String content) {
        if (!StringUtils.hasText(source.getParser())) {
            return content;
        }
        return resolveParser(source.getParser().trim()).parse(content);
    }

    private OutputParser<?> resolveParser(String parserId) {
        if (applicationContext.containsBean(parserId)) {
            Object bean = applicationContext.getBean(parserId);
            if (bean instanceof OutputParser<?> parser) {
                return parser;
            }
            throw new IllegalArgumentException("AI parser bean [" + parserId + "] is not an OutputParser");
        }

        Class<?> parserClass;
        try {
            parserClass = ClassUtils.forName(parserId, applicationContext.getClassLoader());
        }
        catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("AI parser [" + parserId + "] could not be resolved as a bean name or class name", ex);
        }

        if (!OutputParser.class.isAssignableFrom(parserClass)) {
            throw new IllegalArgumentException("AI parser class [" + parserClass.getName() + "] does not implement OutputParser");
        }
        return instantiateParser(parserId, parserClass.asSubclass(OutputParser.class));
    }

    private OutputParser<?> instantiateParser(String parserId, Class<? extends OutputParser> parserType) {
        try {
            return applicationContext.getBean(parserType);
        }
        catch (BeansException ignored) {
            // Fall through to creation paths for class-based parsers that are not registered as beans.
        }

        try {
            return (OutputParser<?>) beanFactory.createBean(parserType);
        }
        catch (BeansException ignored) {
            // Fall through to lightweight reflective instantiation for simple parser implementations.
        }

        Constructor<? extends OutputParser> conversionServiceConstructor = findConversionServiceConstructor(parserType);
        if (conversionServiceConstructor != null) {
            return (OutputParser<?>) org.springframework.beans.BeanUtils.instantiateClass(
                    conversionServiceConstructor,
                    fallbackConversionService
            );
        }

        try {
            return org.springframework.beans.BeanUtils.instantiateClass(parserType);
        }
        catch (BeanInstantiationException ex) {
            throw new IllegalArgumentException("AI parser [" + parserId + "] could not be instantiated", ex);
        }
    }

    private Constructor<? extends OutputParser> findConversionServiceConstructor(Class<? extends OutputParser> parserType) {
        for (Constructor<?> constructor : parserType.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(DefaultConversionService.class)) {
                @SuppressWarnings("unchecked")
                Constructor<? extends OutputParser> matched = (Constructor<? extends OutputParser>) constructor;
                return matched;
            }
        }
        return null;
    }
}
