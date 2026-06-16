/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.config.DataGeneratorProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiPricingService}.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
class AiPricingServiceTests {

    @Test
    void estimateUsdUsesBuiltInOpenAiModelRates() {
        AiPricingService service = new AiPricingService(new DataGeneratorProperties());

        double cost = service.estimateUsd("OPENAI", "gpt-4o-mini", 1_000_000L, 1_000_000L);

        Assertions.assertEquals(0.75D, cost, 0.000001D);
    }

    @Test
    void estimateUsdFallsBackToProviderWildcard() {
        AiPricingService service = new AiPricingService(new DataGeneratorProperties());

        double cost = service.estimateUsd("OPENAI", "unknown-model", 1_000_000L, 0L);

        Assertions.assertEquals(2.50D, cost, 0.000001D);
    }

    @Test
    void configuredPricingOverridesBuiltInDefaults() {
        DataGeneratorProperties properties = new DataGeneratorProperties();
        DataGeneratorProperties.AiModelPricingEntry entry = new DataGeneratorProperties.AiModelPricingEntry();
        entry.setProviderType("OPENAI");
        entry.setModel("custom-model");
        entry.setPromptUsdPer1M(1.0D);
        entry.setCompletionUsdPer1M(2.0D);
        properties.getAiRuntime().getModelPricing().add(entry);

        AiPricingService service = new AiPricingService(properties);
        double cost = service.estimateUsd("OPENAI", "custom-model", 1_000_000L, 1_000_000L);

        Assertions.assertEquals(3.0D, cost, 0.000001D);
        Assertions.assertTrue(service.listPricing().stream()
                .anyMatch(row -> "custom-model".equals(row.model()) && row.configured()));
    }
}
