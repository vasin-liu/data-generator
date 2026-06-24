/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.datasource.api.CatalogEntry;

/**
 * Catalog list row for console datasource overview (D-26).
 *
 * @param name   connection name
 * @param kind   JDBC, KAFKA, or ELASTICSEARCH
 * @param source BOOTSTRAP or MANAGED
 */
public record CatalogConnectionSummaryDto(String name, String kind, String source) {

    /**
     * @param entry catalog entry
     * @return console DTO
     */
    public static CatalogConnectionSummaryDto from(CatalogEntry entry) {
        return new CatalogConnectionSummaryDto(
                entry.name(),
                entry.kind().name(),
                entry.source().name());
    }
}
