/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.datasource.catalog.CatalogBootstrapSupport;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Loads enabled {@code datasource_config} rows into {@link com.baomidou.dynamic.datasource.DynamicRoutingDataSource} at startup.
 * YAML bootstrap catalog entries are registered earlier via {@link CatalogBootstrapSupport} (D-25).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class DataSourceBootstrap implements ApplicationRunner {

    private final DataSourceConfigService dataSourceConfigService;
    private final CatalogBootstrapSupport catalogBootstrapSupport;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Ensure bootstrap catalog entries are present before managed JDBC pools overlay yaml keys.
            catalogBootstrapSupport.registerBootstrapEntries();
            dataSourceConfigService.bootstrapEnabled();
            log.info("Datasource config bootstrap completed");
        } catch (Exception e) {
            log.warn("Datasource config bootstrap skipped or partial: {}", e.getMessage());
        }
    }
}
