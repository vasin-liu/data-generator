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
import org.gensokyo.data.template.migration.MigrationBatchCompareOptions;
import org.gensokyo.data.template.migration.MigrationBatchCompareResult;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.migration.MigrationReportWriter;
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
 * Integration tests for {@code POST /template/migration/compare/batch}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TemplateControllerMigrationBatchCompareTests.BatchCompareTestConfig.class)
class TemplateControllerMigrationBatchCompareTests {

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
    void batchCompareMigrationComparesUnclassifiedDatabaseTemplates() {
        TemplatePO entity = new TemplatePO();
        entity.setId(95010L);
        entity.setName("batch-compare-number");
        entity.setContentYaml("""
                name: batch-compare-number
                iterator:
                  type: number
                  from: 1
                  to: 2
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        MigrationInventoryEntry entry = new MigrationInventoryEntry();
        entry.setId("db-" + entity.getId());
        entry.setName(entity.getName());
        entry.setOrigin("database");
        entry.setDbTemplateId(entity.getId());
        entry.setMigrationClass(MigrationClassification.UNCLASSIFIED);
        migrationInventoryService.saveAll(List.of(entry));

        MigrationBatchCompareOptions options = MigrationBatchCompareOptions.defaults();
        options.setRefreshInventoryFirst(false);
        options.setMaxTemplates(5);

        R<MigrationBatchCompareResult> result = templateController.batchCompareMigration(options);

        Assertions.assertTrue(result.isSuccess());
        MigrationBatchCompareResult batch = result.getData();
        Assertions.assertNotNull(batch);
        Assertions.assertEquals(1, batch.getComparedCount());
        Assertions.assertTrue(batch.getItems().stream()
                .anyMatch(i -> ("db-" + entity.getId()).equals(i.getInventoryId())));
    }

    @Test
    void batchCompareMigrationSkipsCompatibilityOnlyRows() {
        TemplatePO skipped = new TemplatePO();
        skipped.setId(95011L);
        skipped.setName("batch-skip-compat");
        skipped.setContentYaml("""
                name: batch-skip-compat
                iterator:
                  type: number
                  from: 1
                  to: 1
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(skipped);

        TemplatePO compared = new TemplatePO();
        compared.setId(95012L);
        compared.setName("batch-compare-active");
        compared.setContentYaml("""
                name: batch-compare-active
                iterator:
                  type: number
                  from: 1
                  to: 2
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(compared);

        MigrationInventoryEntry compat = new MigrationInventoryEntry();
        compat.setId("db-" + skipped.getId());
        compat.setName(skipped.getName());
        compat.setOrigin("database");
        compat.setDbTemplateId(skipped.getId());
        compat.setMigrationClass(MigrationClassification.COMPATIBILITY_ONLY);

        MigrationInventoryEntry active = new MigrationInventoryEntry();
        active.setId("db-" + compared.getId());
        active.setName(compared.getName());
        active.setOrigin("database");
        active.setDbTemplateId(compared.getId());
        active.setMigrationClass(MigrationClassification.UNCLASSIFIED);

        migrationInventoryService.saveAll(List.of(compat, active));

        MigrationBatchCompareOptions options = MigrationBatchCompareOptions.defaults();
        options.setRefreshInventoryFirst(false);
        options.setSkipCompatibilityOnly(true);
        options.setMaxTemplates(10);

        R<MigrationBatchCompareResult> result = templateController.batchCompareMigration(options);

        Assertions.assertTrue(result.isSuccess());
        MigrationBatchCompareResult batch = result.getData();
        Assertions.assertEquals(1, batch.getComparedCount());
        Assertions.assertEquals(1, batch.getSkippedCount());
    }

    @TestConfiguration
    static class BatchCompareTestConfig {

        @Bean
        @Primary
        MigrationInventoryService testMigrationInventoryService() throws Exception {
            Path inventoryPath = Files.createTempFile("inventory-batch", ".yaml");
            Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
            return new MigrationInventoryService(inventoryPath);
        }

        @Bean
        @Primary
        MigrationReportWriter testMigrationReportWriter() throws Exception {
            Path reportsDir = Files.createTempDirectory("migration-reports-batch");
            return new MigrationReportWriter(reportsDir);
        }
    }
}
