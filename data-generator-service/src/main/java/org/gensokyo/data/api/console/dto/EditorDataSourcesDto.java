/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;

/**
 * Runtime connection keys for template editor dropdowns (JDBC, Kafka, Elasticsearch).
 *
 * @param jdbcNames           JDBC keys from dynamic datasource registry
 * @param kafkaClusters       Kafka cluster ids from {@code spring.kafka.multiple}
 * @param elasticsearchClusters Elasticsearch cluster ids from {@code spring.elasticsearch.multiple}
 * @author Gensokyo
 * @since 2026-06-03
 */
public record EditorDataSourcesDto(
        List<String> jdbcNames, List<String> kafkaClusters, List<String> elasticsearchClusters) {
}
