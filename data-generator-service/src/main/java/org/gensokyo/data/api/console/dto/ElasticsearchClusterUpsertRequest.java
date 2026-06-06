/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;

/**
 * Request body for console-managed Elasticsearch cluster upsert.
 *
 * @param name                unique cluster id
 * @param uris                node URIs
 * @param username            optional basic auth user
 * @param password            optional basic auth password; blank on edit keeps existing
 * @param apiKey              optional API key auth; blank on edit keeps existing
 * @param pathPrefix          optional HTTP path prefix
 * @param connectionTimeoutMs optional connect timeout in milliseconds
 * @param socketTimeoutMs     optional socket timeout in milliseconds
 * @param socketKeepAlive     optional TCP keep-alive flag
 */
public record ElasticsearchClusterUpsertRequest(
        String name,
        List<String> uris,
        String username,
        String password,
        String apiKey,
        String pathPrefix,
        Integer connectionTimeoutMs,
        Integer socketTimeoutMs,
        Boolean socketKeepAlive) {
}
