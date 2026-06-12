/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.catalog;

import org.gensokyo.data.ai.parser.ListOutputParser;
import org.gensokyo.data.api.console.dto.AiCatalogDto;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiCatalogService}.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
class AiCatalogServiceTest {

    private final AiCatalogService service = new AiCatalogService(new JacksonParser());

    @Test
    void catalog_listsProvidersParsersAndPromptTemplates() {
        AiCatalogDto catalog = service.catalog();

        Assertions.assertTrue(catalog.providers().stream().anyMatch(row -> "INLINE".equals(row.type())));
        Assertions.assertTrue(catalog.providers().stream().anyMatch(row -> "OLLAMA".equals(row.type())));
        Assertions.assertTrue(catalog.parsers().stream()
                .anyMatch(row -> ListOutputParser.class.getName().equals(row.id())));
        Assertions.assertTrue(catalog.promptTemplates().stream()
                .anyMatch(row -> "training-titles-zh".equals(row.id())));
    }
}
