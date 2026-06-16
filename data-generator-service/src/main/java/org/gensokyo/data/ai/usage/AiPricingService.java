/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.config.DataGeneratorProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolves per-model token pricing and estimates USD cost for AI call metrics.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@Service
public class AiPricingService {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);
    private static final String WILDCARD_MODEL = "*";

    private final List<ModelRate> effectiveRates;

    /**
     * @param properties platform AI runtime configuration
     */
    public AiPricingService(DataGeneratorProperties properties) {
        DataGeneratorProperties.AiRuntime aiRuntime = properties == null
                ? new DataGeneratorProperties.AiRuntime()
                : properties.getAiRuntime();
        List<ModelRate> configured = parseConfigured(aiRuntime == null ? List.of() : aiRuntime.getModelPricing());
        this.effectiveRates = mergeWithBuiltInDefaults(configured);
    }

    /**
     * @return operator-facing pricing table (configured overrides plus built-in defaults)
     */
    public List<AiModelPricingView> listPricing() {
        List<AiModelPricingView> views = new ArrayList<>();
        for (ModelRate rate : effectiveRates) {
            views.add(new AiModelPricingView(
                    rate.providerType(),
                    rate.model(),
                    rate.promptUsdPer1M().doubleValue(),
                    rate.completionUsdPer1M().doubleValue(),
                    rate.configured()));
        }
        return List.copyOf(views);
    }

    /**
     * Estimates USD cost for a single AI call.
     *
     * @param providerType      provider identifier
     * @param model             resolved model name
     * @param promptTokens      prompt token count
     * @param completionTokens  completion token count
     * @return estimated USD cost (0 when no rate matches)
     */
    public double estimateUsd(String providerType, String model, long promptTokens, long completionTokens) {
        ModelRate rate = resolve(providerType, model);
        if (rate == null) {
            return 0.0D;
        }
        BigDecimal promptCost = rate.promptUsdPer1M()
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal completionCost = rate.completionUsdPer1M()
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    private ModelRate resolve(String providerType, String model) {
        String normalizedProvider = normalizeProvider(providerType);
        String normalizedModel = normalizeModel(model);
        ModelRate exact = findRate(normalizedProvider, normalizedModel);
        if (exact != null) {
            return exact;
        }
        return findRate(normalizedProvider, WILDCARD_MODEL);
    }

    private ModelRate findRate(String providerType, String model) {
        for (ModelRate rate : effectiveRates) {
            if (rate.providerType().equals(providerType) && rate.model().equals(model)) {
                return rate;
            }
        }
        return null;
    }

    private static List<ModelRate> mergeWithBuiltInDefaults(List<ModelRate> configured) {
        List<ModelRate> merged = new ArrayList<>(builtInDefaults());
        for (ModelRate override : configured) {
            merged.removeIf(rate -> rate.providerType().equals(override.providerType())
                    && rate.model().equals(override.model()));
            merged.add(override);
        }
        return List.copyOf(merged);
    }

    private static List<ModelRate> builtInDefaults() {
        return List.of(
                rate("OPENAI", "gpt-4o-mini", "0.15", "0.60", false),
                rate("OPENAI", "gpt-4o", "2.50", "10.00", false),
                rate("OPENAI", WILDCARD_MODEL, "2.50", "10.00", false),
                rate("AZURE_OPENAI", WILDCARD_MODEL, "2.50", "10.00", false),
                rate("OLLAMA", WILDCARD_MODEL, "0", "0", false));
    }

    private static List<ModelRate> parseConfigured(List<DataGeneratorProperties.AiModelPricingEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<ModelRate> parsed = new ArrayList<>();
        for (DataGeneratorProperties.AiModelPricingEntry entry : entries) {
            if (entry == null || entry.getProviderType() == null || entry.getProviderType().isBlank()) {
                continue;
            }
            String model = entry.getModel() == null || entry.getModel().isBlank()
                    ? WILDCARD_MODEL
                    : entry.getModel().trim();
            parsed.add(new ModelRate(
                    normalizeProvider(entry.getProviderType()),
                    normalizeModel(model),
                    decimal(entry.getPromptUsdPer1M()),
                    decimal(entry.getCompletionUsdPer1M()),
                    true));
        }
        return List.copyOf(parsed);
    }

    private static ModelRate rate(String providerType, String model, String prompt, String completion, boolean configured) {
        return new ModelRate(
                normalizeProvider(providerType),
                normalizeModel(model),
                new BigDecimal(prompt),
                new BigDecimal(completion),
                configured);
    }

    private static BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private static String normalizeProvider(String providerType) {
        return providerType == null ? "UNKNOWN" : providerType.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return WILDCARD_MODEL;
        }
        return model.trim();
    }

    private record ModelRate(
            String providerType,
            String model,
            BigDecimal promptUsdPer1M,
            BigDecimal completionUsdPer1M,
            boolean configured) {
    }

    /**
     * Console-facing pricing row.
     *
     * @param providerType        provider identifier
     * @param model               model name or {@code *} wildcard
     * @param promptUsdPer1M      USD per 1M prompt tokens
     * @param completionUsdPer1M  USD per 1M completion tokens
     * @param configured          whether the row comes from application config
     */
    public record AiModelPricingView(
            String providerType,
            String model,
            double promptUsdPer1M,
            double completionUsdPer1M,
            boolean configured) {
    }
}
