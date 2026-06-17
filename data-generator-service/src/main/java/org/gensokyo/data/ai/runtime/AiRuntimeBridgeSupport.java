/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.runtime;

import org.gensokyo.data.ai.usage.AiQuotaService;
import org.gensokyo.data.ai.parser.OutputParser;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.v2.AiSourceVO;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shared parser resolution and retry helpers for remote AI runtime bridges.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
final class AiRuntimeBridgeSupport {

    private final ApplicationContext applicationContext;
    private final AutowireCapableBeanFactory beanFactory;
    private final AiRateLimiter rateLimiter;
    private final AiQuotaService aiQuotaService;
    private final DataGeneratorProperties.AiRuntime rateLimitDefaults;
    private final DefaultConversionService fallbackConversionService = new DefaultConversionService();

    AiRuntimeBridgeSupport(ApplicationContext applicationContext,
                           AutowireCapableBeanFactory beanFactory,
                           AiRateLimiter rateLimiter,
                           AiQuotaService aiQuotaService,
                           DataGeneratorProperties.AiRuntime rateLimitDefaults) {
        this.applicationContext = applicationContext;
        this.beanFactory = beanFactory;
        this.rateLimiter = rateLimiter == null ? new InMemoryAiRateLimiter() : rateLimiter;
        this.aiQuotaService = aiQuotaService;
        this.rateLimitDefaults = rateLimitDefaults == null ? new DataGeneratorProperties.AiRuntime() : rateLimitDefaults;
    }

    /**
     * Parses model output using the configured parser when present.
     *
     * @param source  AI source definition
     * @param content raw provider text
     * @return parsed payload or raw text
     */
    Object parse(AiSourceVO source, String content) {
        if (!StringUtils.hasText(source.getParser())) {
            return content;
        }
        return resolveParser(source.getParser().trim()).parse(content);
    }

    /**
     * Executes a remote call with optional rate limiting and retry/backoff from provider options.
     *
     * @param providerType    provider identifier for quota accounting
     * @param model           resolved model name for quota cost accounting
     * @param rateLimitKey    throttle bucket key
     * @param providerOptions provider option map
     * @param call            remote call supplier
     * @return successful call payload and metrics
     */
    RemoteAiContentCall executeWithRetry(
            String providerType,
            String model,
            String rateLimitKey,
            Map<String, Object> providerOptions,
            Supplier<RemoteAiContentCall> call) {
        rateLimiter.acquire(rateLimitKey, AiRateLimitPolicy.resolve(providerOptions, rateLimitDefaults));
        if (aiQuotaService != null) {
            aiQuotaService.beforeCall(providerType);
        }
        int maxRetries = intOption(providerOptions, "maxRetries", 1);
        long retryBackoffMs = longOption(providerOptions, "retryBackoffMs", 0L);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                long startNanos = System.nanoTime();
                RemoteAiContentCall result = call.get();
                long latencyMs = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
                RemoteAiContentCall traced = result.withAttemptsAndLatency(attempt, latencyMs);
                if (aiQuotaService != null) {
                    aiQuotaService.recordUsage(
                            providerType,
                            model,
                            traced.promptTokens(),
                            traced.completionTokens());
                }
                return traced;
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

    static int intOption(Map<String, Object> options, String key, int defaultValue) {
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

    static long longOption(Map<String, Object> options, String key, long defaultValue) {
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

    static String stringOption(Map<String, Object> options, String key) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return null;
        }
        String value = String.valueOf(options.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Builds a stable throttle bucket key for a remote provider call.
     *
     * @param source       AI source definition
     * @param providerType normalized provider type
     * @param options      provider options
     * @return limiter key
     */
    static String rateLimitKey(AiSourceVO source, String providerType, Map<String, Object> options) {
        String explicit = stringOption(options, "rateLimitKey");
        if (explicit != null) {
            return providerType + ":" + explicit;
        }
        String model = stringOption(options, "model");
        if (model != null) {
            return providerType + ":" + model;
        }
        if (source != null && source.getApi() != null && !source.getApi().isBlank()) {
            return providerType + ":" + source.getApi().trim();
        }
        return providerType + ":default";
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
