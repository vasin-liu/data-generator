/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.datasource.DataSourceConfigSummary;

import java.util.List;

/**
 * Persisted JDBC configs plus runtime registry keys for the datasources page.
 *
 * @param persisted   rows from {@code datasource_config}
 * @param runtimeKeys merged yaml + persisted keys
 */
public record DataSourcesOverviewDto(
        List<DataSourceConfigSummary> persisted,
        List<String> runtimeKeys) {
}
