/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for catalog bootstrap wiring (D-25).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class ConnectionCatalogBootstrapTests {

    @Autowired
    private ConnectionCatalog connectionCatalog;

    @Test
    void listAll_includesYamlBootstrapJdbcEntries() {
        boolean hasBootstrapJdbc = connectionCatalog.listAll().stream()
                .anyMatch(entry -> entry.kind() == ConnectionKind.JDBC
                        && entry.source() == CatalogEntrySource.BOOTSTRAP
                        && "data-generator".equals(entry.name()));
        Assertions.assertTrue(hasBootstrapJdbc, "Expected yaml bootstrap JDBC entry data-generator");
    }

    @Test
    void resolveJdbc_returnsHandleForBootstrapName() {
        CatalogEntry bootstrap = connectionCatalog.listAll().stream()
                .filter(entry -> "data-generator".equals(entry.name()))
                .findFirst()
                .orElseThrow();
        Assertions.assertDoesNotThrow(() ->
                connectionCatalog.resolve(bootstrap.name(), ConnectionKind.JDBC));
    }
}
