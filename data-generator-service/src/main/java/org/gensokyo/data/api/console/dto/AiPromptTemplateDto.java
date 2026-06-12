/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Bundled AI prompt template for the console source editor.
 *
 * @param id     stable template id
 * @param label  operator-facing label
 * @param prompt prompt text applied to {@code AiSourceVO.prompt}
 * @author Gensokyo
 * @since 2026-06-11
 */
public record AiPromptTemplateDto(
        String id,
        String label,
        String prompt) {
}
