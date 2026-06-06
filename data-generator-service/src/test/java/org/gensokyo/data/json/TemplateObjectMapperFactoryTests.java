/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.json;

import org.gensokyo.data.api.console.dto.TemplateSummaryDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies console HTTP JSON encodes snowflake ids as strings (JavaScript safe integers).
 *
 * @author Gensokyo
 * @since 2026-06-05
 */
class TemplateObjectMapperFactoryTests {

    @Test
    void serializesLongIdsAsJsonStrings() throws Exception {
        TemplateSummaryDto dto = new TemplateSummaryDto(
                19_696_372_225_036_290L, "demo", "PUBLISHED", false, null, null);
        String json = TemplateObjectMapperFactory.buildJsonMapper().writeValueAsString(dto);
        assertTrue(json.contains("\"19696372225036290\""), () -> "expected string id in: " + json);
    }
}
