/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;

/**
 * AI authoring catalog for the operator console (providers, parsers, prompt templates).
 *
 * @param providers       supported provider adapters
 * @param parsers         registered output parsers
 * @param promptTemplates bundled prompt templates
 * @author Gensokyo
 * @since 2026-06-11
 */
public record AiCatalogDto(
        List<AiProviderEntryDto> providers,
        List<AiParserEntryDto> parsers,
        List<AiPromptTemplateDto> promptTemplates) {
}
