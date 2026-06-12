/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Registered AI output parser for the console source editor.
 *
 * @param id          bean name or fully qualified class name stored on {@code AiSourceVO.parser}
 * @param label       operator-facing label
 * @param description short usage hint
 * @author Gensokyo
 * @since 2026-06-11
 */
public record AiParserEntryDto(
        String id,
        String label,
        String description) {
}
