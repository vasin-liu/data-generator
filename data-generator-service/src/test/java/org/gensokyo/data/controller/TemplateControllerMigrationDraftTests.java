/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.migration.MigrationBusinessSignoffRequest;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.querysource.V1QuerySourceExecutionPolicySuggester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Integration tests for unified migration draft and promote endpoints.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TemplateControllerMigrationDraftTests.MigrationDraftTestConfig.class)
class TemplateControllerMigrationDraftTests {

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
    void buildMigrationDraftForIteratorFixture() throws Exception {
        String yaml = new ClassPathResource("migration/regression/v1-iterator-simple.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        TemplatePO entity = new TemplatePO();
        entity.setId(94001L);
        entity.setName("iterator-draft");
        entity.setContentYaml(yaml);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.buildMigrationDraft(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("Draft generated", result.getMessage());
        Assertions.assertInstanceOf(IteratorSourceVO.class, result.getData().getSources().get("input"));
        Assertions.assertTrue(result.getData().getTransformers().stream().anyMatch(SqlTransformVO.class::isInstance));
        Assertions.assertTrue(result.getData().getTransformers().stream().anyMatch(SpelTransformVO.class::isInstance));
    }

    @Test
    void jdbcMigrateDraftIncludesChunkedExecutionPolicy() {
        TemplatePO entity = new TemplatePO();
        entity.setId(94002L);
        entity.setName("jdbc-chunked");
        entity.setContentYaml("""
                name: jdbc-chunked
                iterator:
                  type: database
                  dataSourceId: ds_chunk
                  sql: select id from t_chunk
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.migrateQuerySourceV2ById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        ExecutionPolicyVO policy = result.getData().getExecutionPolicy();
        Assertions.assertNotNull(policy);
        Assertions.assertEquals("CHUNKED", policy.getMode());
        Assertions.assertEquals(V1QuerySourceExecutionPolicySuggester.DEFAULT_SOURCE_CHUNK_SIZE, policy.getSourceChunkSize());
    }

    @Test
    void promoteRequiresValidDraft() {
        TemplatePO entity = new TemplatePO();
        entity.setId(94003L);
        entity.setName("invalid-promote");
        entity.setContentYaml("""
                name: invalid-promote
                iterator:
                  type: excel
                  path: missing.xlsx
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2DraftVO> result = templateController.promoteMigration(entity.getId());

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertNotNull(result.getMessage());
    }

    @Test
    void promoteRejectsCompatibilityOnlyClassification() {
        TemplatePO entity = new TemplatePO();
        entity.setId(94005L);
        entity.setName("compat-only-promote");
        entity.setContentYaml("""
                name: compat-only-promote
                iterator:
                  type: number
                  from: 1
                  to: 2
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        MigrationComparisonReport report = new MigrationComparisonReport();
        report.setTemplateId(entity.getId());
        report.setClassification(MigrationClassification.COMPATIBILITY_ONLY);
        report.applyRecommendationFromClassification();
        migrationInventoryService.updateCompareResult(
                entity.getId(), report, "docs/migration/reports/compat-only.md");

        R<TemplateV2DraftVO> result = templateController.promoteMigration(entity.getId());

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("COMPATIBILITY_ONLY"));

        TemplatePO persisted = templateRepository.findById(entity.getId()).orElseThrow();
        Assertions.assertTrue(persisted.getContentYaml().contains("iterator:"));
        Assertions.assertFalse(persisted.getContentYaml().contains("sources:"));
    }

    @Test
    void promoteUpdatesInventoryClassificationFromLastCompare() throws Exception {
        TemplatePO entity = new TemplatePO();
        entity.setId(94004L);
        entity.setName("promote-with-compare");
        entity.setContentYaml("""
                name: promote-with-compare
                iterator:
                  type: number
                  from: 1
                  to: 2
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        MigrationComparisonReport report = new MigrationComparisonReport();
        report.setTemplateId(entity.getId());
        report.setClassification(MigrationClassification.EXACT);
        report.setV1RowCount(2);
        report.setV2RowCount(2);
        report.setSampleSize(2);
        report.setSampleMatchRate(1.0);
        report.applyRecommendationFromClassification();
        migrationInventoryService.updateCompareResult(entity.getId(), report, "docs/migration/reports/sample-promote.md");

        MigrationBusinessSignoffRequest signoff = new MigrationBusinessSignoffRequest();
        signoff.setApproved(true);
        signoff.setApprovedBy("draft-test");
        templateController.recordMigrationSignoff("db-" + entity.getId(), signoff);

        R<TemplateV2DraftVO> promoted = templateController.promoteMigration(entity.getId());

        Assertions.assertTrue(promoted.isSuccess());
        MigrationInventoryEntry entry = migrationInventoryService.findById("db-" + entity.getId()).orElseThrow();
        Assertions.assertEquals(MigrationClassification.EXACT, entry.getMigrationClass());
        Assertions.assertTrue(entry.isV2DraftPresent());

        TemplatePO persisted = templateRepository.findById(entity.getId()).orElseThrow();
        String yaml = persisted.getContentYaml();
        Assertions.assertTrue(yaml.contains("sources:"));
        Assertions.assertTrue(yaml.contains("transform:"));
        Assertions.assertTrue(yaml.contains("SELECT * FROM input"));
    }

    @TestConfiguration
    static class MigrationDraftTestConfig {

        @Bean
        @Primary
        MigrationInventoryService testMigrationInventoryService() throws Exception {
            Path inventoryPath = Files.createTempFile("inventory-draft", ".yaml");
            Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
            return new MigrationInventoryService(inventoryPath);
        }
    }
}
