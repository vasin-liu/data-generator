/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;

/**
 * Distinct template categories and tags for catalog filters and editor pickers.
 *
 * @param categories sorted category names
 * @param tags       sorted tag names
 * @author Gensokyo
 * @since 2026-06-04
 */
public record TemplateTaxonomyDto(List<String> categories, List<String> tags) {
}
