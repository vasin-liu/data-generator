/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

/**
 * Kafka list metadata without credentials (D-10).
 *
 * @param bootstrapServers comma-separated bootstrap servers for operator display
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public record KafkaCatalogMetadata(String bootstrapServers) implements CatalogMetadata {
}
