/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Known AI provider adapter for the console source editor.
 *
 * @param type        provider type id (e.g. {@code INLINE}, {@code OLLAMA})
 * @param label       operator-facing label
 * @param description short usage hint
 * @param remote      whether the provider calls an external runtime bridge
 * @author Gensokyo
 * @since 2026-06-11
 */
public record AiProviderEntryDto(
        String type,
        String label,
        String description,
        boolean remote) {
}
