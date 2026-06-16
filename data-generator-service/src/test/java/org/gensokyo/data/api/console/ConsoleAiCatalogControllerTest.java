/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.ai.catalog.AiCatalogService;
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
        when(usageService.summarize()).thenReturn(
                new org.gensokyo.data.api.console.dto.AiUsageSummaryDto(0L, 0L, 0L, 0L, 0L, java.util.List.of()));
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsoleAiCatalogController(catalogService, usageService)).build();
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
                .andExpect(jsonPath("$.data.totalCalls").value(0));
    }
}
