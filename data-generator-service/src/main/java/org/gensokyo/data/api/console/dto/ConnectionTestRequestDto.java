/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.Map;

/**
 * Unified connectivity test body for JDBC, Kafka, and Elasticsearch (D-18).
 *
 * @param kind          JDBC, KAFKA, or ELASTICSEARCH
 * @param name          catalog entry name when testing a persisted connection
 * @param draftPayload  kind-specific draft map when testing before save
 * @author Gensokyo
 * @since 2026-06-27
 */
public record ConnectionTestRequestDto(
        String kind,
        String name,
        Map<String, Object> draftPayload) {
}
