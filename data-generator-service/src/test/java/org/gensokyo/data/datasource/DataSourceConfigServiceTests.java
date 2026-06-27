/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests for {@link DataSourceConfigService} persistence and runtime registration.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class DataSourceConfigServiceTests {

    private static final String DS_NAME = "ui-test-h2";

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Autowired
    private DataSourceConfigRepository repository;

    @AfterEach
    void tearDown() {
        dataSourceConfigService.remove(DS_NAME);
        repository.findById(DS_NAME).ifPresent(repository::delete);
    }

    @Test
    void saveRegistersRuntimeAndPersists() {
        dataSourceConfigService.save(
                DS_NAME,
                "jdbc:h2:mem:ui_test_ds;DB_CLOSE_DELAY=-1",
                "sa",
                "",
                null,
                "org.h2.Driver",
                null);
        Assertions.assertTrue(dataSourceConfigService.listRuntimeNames().contains(DS_NAME));
        Assertions.assertTrue(repository.findById(DS_NAME).isPresent());
        Assertions.assertEquals("JDBC connection OK", dataSourceConfigService.testConnectionByName(DS_NAME));
    }

    @Test
    void removeRejectsBootstrapOnlyYamlDatasource() {
        IllegalArgumentException failure = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> dataSourceConfigService.remove("data-generator"));
        Assertions.assertTrue(failure.getMessage().contains("Bootstrap datasource cannot be removed"));
    }

    @Test
    void removeDisablesPersistedRow() {
        dataSourceConfigService.save(
                DS_NAME,
                "jdbc:h2:mem:ui_test_ds2;DB_CLOSE_DELAY=-1",
                "sa",
                "",
                null,
                "org.h2.Driver",
                null);
        dataSourceConfigService.remove(DS_NAME);
        Assertions.assertFalse(dataSourceConfigService.listRuntimeNames().contains(DS_NAME));
        Assertions.assertTrue(repository.findById(DS_NAME).map(row -> !row.getEnabled()).orElse(false));
    }
}
