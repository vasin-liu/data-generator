/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryService;
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
 * Negative-path integration tests for migration and backlog REST endpoints.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TemplateControllerMigrationApiNegativeTests.NegativeTestConfig.class)
class TemplateControllerMigrationApiNegativeTests {

    @Autowired
    private TemplateController templateController;

    @Test
    void compareMigrationFailsWhenTemplateMissing() {
        R<MigrationComparisonReport> result = templateController.compareMigration(88888888L, null);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void buildMigrationDraftFailsWhenTemplateMissing() {
        R<TemplateV2DraftVO> result = templateController.buildMigrationDraft(88888887L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void migrationBacklogFailsOnUnknownFilter() {
        R<List<MigrationInventoryEntry>> result = templateController.migrationBacklog("not_a_real_filter");

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertNotNull(result.getMessage());
    }

    @Test
    void recordMigrationSignoffFailsForUnknownInventoryId() {
        R<MigrationInventoryEntry> result = templateController.recordMigrationSignoff("missing-row", null);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("Unknown inventory id"));
    }

    @TestConfiguration
    static class NegativeTestConfig {

        @Bean
        @Primary
        MigrationInventoryService testMigrationInventoryService() throws Exception {
            Path inventoryPath = Files.createTempFile("inventory-negative", ".yaml");
            Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
            return new MigrationInventoryService(inventoryPath);
        }
    }
}
