/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.ai.catalog.AiCatalogService;
import org.gensokyo.data.ai.usage.AiUsageService;
import org.gensokyo.data.api.console.dto.AiCatalogDto;
import org.gensokyo.data.api.console.dto.AiUsageSummaryDto;
import org.gensokyo.data.model.vo.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI authoring catalog for the operator console source editor.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
@RestController
@RequestMapping("/api/console")
public class ConsoleAiCatalogController {

    private final AiCatalogService aiCatalogService;
    private final AiUsageService aiUsageService;

    /**
     * @param aiCatalogService bundled AI metadata service
     * @param aiUsageService   platform AI usage aggregator
     */
    public ConsoleAiCatalogController(AiCatalogService aiCatalogService, AiUsageService aiUsageService) {
        this.aiCatalogService = aiCatalogService;
        this.aiUsageService = aiUsageService;
    }

    /**
     * @return providers, parsers, and prompt templates for AI source authoring
     */
    @GetMapping("/ai/catalog")
    public R<AiCatalogDto> catalog() {
        return R.ok(aiCatalogService.catalog());
    }

    /**
     * @return aggregated AI token usage from successful job run reports
     */
    @GetMapping("/ai/usage")
    public R<AiUsageSummaryDto> usage() {
        return R.ok(aiUsageService.summarize());
    }
}
