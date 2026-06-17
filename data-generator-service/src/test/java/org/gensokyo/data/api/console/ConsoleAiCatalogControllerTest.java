/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.ai.catalog.AiCatalogService;
import org.gensokyo.data.ai.usage.AiPricingService;
import org.gensokyo.data.ai.usage.AiQuotaService;
import org.gensokyo.data.ai.usage.AiUsageService;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ConsoleAiCatalogController}.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
class ConsoleAiCatalogControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiCatalogService catalogService = new AiCatalogService(new JacksonParser());
        AiUsageService usageService = mock(AiUsageService.class);
        AiPricingService pricingService = new AiPricingService(new org.gensokyo.data.config.DataGeneratorProperties());
        AiQuotaService quotaService = mock(AiQuotaService.class);
        when(usageService.summarize()).thenReturn(
                new org.gensokyo.data.api.console.dto.AiUsageSummaryDto(
                        0L, 0L, 0L, 0L, 0L, 0.0D, java.util.List.of()));
        when(quotaService.status()).thenReturn(
                new org.gensokyo.data.api.console.dto.AiQuotaStatusDto(
                        false, "2026-06-12", 0L, 0L, 0.0D, 0L, 0L, 0L, 0.0D, null, null, null, false, 80, java.util.List.of()));
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ConsoleAiCatalogController(catalogService, usageService, pricingService, quotaService)).build();
    }

    @Test
    void catalog_returnsBundledAiMetadata() throws Exception {
        mockMvc.perform(get("/api/console/ai/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providers[0].type").exists())
                .andExpect(jsonPath("$.data.parsers[0].id").exists())
                .andExpect(jsonPath("$.data.promptTemplates[0].id").exists());
    }

    @Test
    void usage_returnsAggregatedTotals() throws Exception {
        mockMvc.perform(get("/api/console/ai/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCalls").value(0))
                .andExpect(jsonPath("$.data.estimatedCostUsd").value(0.0));
    }

    @Test
    void pricing_returnsEffectiveModelRates() throws Exception {
        mockMvc.perform(get("/api/console/ai/pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].providerType").exists())
                .andExpect(jsonPath("$.data[0].promptUsdPer1M").exists());
    }

    @Test
    void quota_returnsDailyQuotaStatus() throws Exception {
        mockMvc.perform(get("/api/console/ai/quota"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.usageDate").value("2026-06-12"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }
}
