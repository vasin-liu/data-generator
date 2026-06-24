/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

/**
 * Non-secret list metadata for catalog entries. Implementations are kind-specific display hints only (D-10).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public sealed interface CatalogMetadata permits JdbcCatalogMetadata, KafkaCatalogMetadata, ElasticsearchCatalogMetadata {
}
