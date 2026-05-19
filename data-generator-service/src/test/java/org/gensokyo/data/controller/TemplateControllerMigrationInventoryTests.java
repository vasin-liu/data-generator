/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryRefreshResult;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Integration tests for migration inventory list and refresh endpoints.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TemplateControllerMigrationInventoryTests.InventoryTestConfig.class)
class TemplateControllerMigrationInventoryTests {

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private MigrationInventoryService migrationInventoryService;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void listMigrationInventoryReturnsEntries() {
        R<List<MigrationInventoryEntry>> response = templateController.listMigrationInventory();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertFalse(response.getData().isEmpty());
    }

    @Test
    void refreshMigrationInventoryAddsDbTemplate() {
        TemplatePO entity = new TemplatePO();
        entity.setId(93001L);
        entity.setName("inventory-refresh-api");
        entity.setContentYaml("""
                name: inventory-refresh-api
                iterator:
                  type: database
                  dataSourceId: ds_main
                  sql: select 1
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<MigrationInventoryRefreshResult> response = templateController.refreshMigrationInventory();
        Assertions.assertTrue(response.isSuccess());
        MigrationInventoryRefreshResult result = response.getData();
        Assertions.assertTrue(result.getAddedCount() >= 1);
        Assertions.assertTrue(result.getTotalCount() >= 1);
        Assertions.assertTrue(result.isPersisted());

        MigrationInventoryEntry entry = migrationInventoryService.findById("db-" + entity.getId()).orElseThrow();
        Assertions.assertEquals("database", entry.getOrigin());
        Assertions.assertEquals(entity.getId(), entry.getDbTemplateId());
    }

    @TestConfiguration
    static class InventoryTestConfig {

        @Bean
        @Primary
        MigrationInventoryService testMigrationInventoryService() throws Exception {
            Path inventoryPath = Files.createTempFile("inventory-controller", ".yaml");
            Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
            return new MigrationInventoryService(inventoryPath);
        }
    }
}
