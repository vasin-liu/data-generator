/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.migration.EmbeddedJdbcCompareFixtureSupport;
import org.gensokyo.data.template.migration.MigrationBusinessSignoffRequest;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationCompareOptions;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryRefreshResult;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.migration.MigrationReportWriter;
import org.gensokyo.data.template.migration.MigrationSignoffFamilyStatus;
import org.gensokyo.data.template.migration.TemplateMigrationAnalysisDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Set;

/**
 * CI simulation of the staging runbook through promote: refresh, analyze, draft, compare, sign-off, promote.
 * Uses phase7-test profile and embedded H2 instead of a live staging server.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(StagingSimulatedPromoteWorkflowTests.StagingSimulatedConfig.class)
class StagingSimulatedPromoteWorkflowTests {

    private static final Set<MigrationClassification> PROMOTABLE_COMPARE_CLASSES = Set.of(
            MigrationClassification.EXACT,
            MigrationClassification.ADAPTED,
            MigrationClassification.APPROXIMATE);

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private MigrationInventoryService migrationInventoryService;

    @BeforeEach
    void seedEmbeddedJdbcData() throws Exception {
        // Minimal id-only table — staging-sim-jdbc has no SCRIPT fields; parking/11 CI uses full seed via support.
        EmbeddedJdbcCompareFixtureSupport.seedParkingCompareTable();
    }

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void syntheticRegressionTemplateStagingWorkflowPromotesToV2() throws Exception {
        String yaml = new ClassPathResource("migration/regression/v1-iterator-simple.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        TemplatePO entity = new TemplatePO();
        entity.setId(95101L);
        entity.setName("staging-sim-synthetic");
        entity.setContentYaml(yaml);
        templateRepository.saveAndFlush(entity);

        runStagingWorkflowAndPromote(entity);
    }

    @Test
    void jdbcShapedTemplateStagingWorkflowPromotesToV2() {
        TemplatePO entity = new TemplatePO();
        entity.setId(95102L);
        entity.setName("staging-sim-jdbc");
        entity.setContentYaml("""
                name: staging-sim-jdbc
                iterator:
                  type: database
                  dataSourceId: compare-inline-ds
                  sql: select id from t_compare order by id
                  maxRows: 100
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        runStagingWorkflowAndPromote(entity);
    }

    @Test
    void stagingWorkflowRejectsPromoteWithoutBusinessSignoffAfterCompare() throws Exception {
        String yaml = new org.springframework.core.io.ClassPathResource(
                "migration/regression/v1-iterator-simple.yaml")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        TemplatePO entity = new TemplatePO();
        entity.setId(95104L);
        entity.setName("staging-sim-unsigned");
        entity.setContentYaml(yaml);
        templateRepository.saveAndFlush(entity);

        Assertions.assertTrue(templateController.refreshMigrationInventory().isSuccess());
        Assertions.assertTrue(templateController.compareMigration(
                entity.getId(), MigrationCompareOptions.defaults()).isSuccess());

        R<TemplateV2DraftVO> promoted = templateController.promoteMigration(entity.getId());
        Assertions.assertFalse(promoted.isSuccess());
        Assertions.assertTrue(
                promoted.getMessage().toLowerCase().contains("sign-off")
                        || promoted.getMessage().toLowerCase().contains("signoff"));

        TemplatePO persisted = templateRepository.findById(entity.getId()).orElseThrow();
        Assertions.assertTrue(persisted.getContentYaml().contains("iterator:"));
        Assertions.assertFalse(persisted.getContentYaml().contains("sources:"));
    }

    @Test
    void stagingWorkflowDoesNotPromoteCompatibilityOnlyAfterCompare() {
        TemplatePO entity = new TemplatePO();
        entity.setId(95103L);
        entity.setName("staging-sim-compat");
        entity.setContentYaml("""
                name: staging-sim-compat
                iterator:
                  type: number
                  from: 1
                  to: 2
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<MigrationInventoryRefreshResult> refresh = templateController.refreshMigrationInventory();
        Assertions.assertTrue(refresh.isSuccess());

        MigrationComparisonReport report = new MigrationComparisonReport();
        report.setTemplateId(entity.getId());
        report.setClassification(MigrationClassification.COMPATIBILITY_ONLY);
        report.applyRecommendationFromClassification();
        migrationInventoryService.updateCompareResult(
                entity.getId(), report, "docs/migration/reports/staging-sim-compat.md");

        MigrationBusinessSignoffRequest signoff = new MigrationBusinessSignoffRequest();
        signoff.setApproved(true);
        signoff.setApprovedBy("staging-sim-test");
        R<MigrationInventoryEntry> signed = templateController.recordMigrationSignoff(
                "db-" + entity.getId(), signoff);
        Assertions.assertTrue(signed.isSuccess());

        R<TemplateV2DraftVO> promoted = templateController.promoteMigration(entity.getId());
        Assertions.assertFalse(promoted.isSuccess());
        Assertions.assertTrue(promoted.getMessage().contains("COMPATIBILITY_ONLY"));

        TemplatePO persisted = templateRepository.findById(entity.getId()).orElseThrow();
        Assertions.assertTrue(persisted.getContentYaml().contains("iterator:"));
        Assertions.assertFalse(persisted.getContentYaml().contains("sources:"));
    }

    /**
     * Mirrors {@code scripts/migration-staging.ps1 -Action workflow} plus sign-off and promote.
     *
     * @param entity persisted V1 template row (already saved)
     */
    private void runStagingWorkflowAndPromote(TemplatePO entity) {
        String inventoryId = "db-" + entity.getId();

        R<MigrationInventoryRefreshResult> refresh = templateController.refreshMigrationInventory();
        Assertions.assertTrue(refresh.isSuccess(), refresh.getMessage());
        Assertions.assertTrue(
                migrationInventoryService.findById(inventoryId).isPresent(),
                () -> "inventory row missing after refresh: " + inventoryId);

        R<TemplateMigrationAnalysisDTO> analyze = templateController.analyzeMigration(entity.getId());
        Assertions.assertTrue(analyze.isSuccess(), analyze.getMessage());
        Assertions.assertNotEquals(
                MigrationClassification.COMPATIBILITY_ONLY,
                analyze.getData().getSuggestedClass());

        R<TemplateV2DraftVO> draft = templateController.buildMigrationDraft(entity.getId());
        Assertions.assertTrue(draft.isSuccess(), draft.getMessage());

        R<MigrationComparisonReport> compare = templateController.compareMigration(
                entity.getId(),
                MigrationCompareOptions.defaults());
        Assertions.assertTrue(compare.isSuccess(), compare.getMessage());
        MigrationClassification compareClass = compare.getData().getClassification();
        Assertions.assertTrue(
                PROMOTABLE_COMPARE_CLASSES.contains(compareClass),
                () -> "compare class " + compareClass + " warnings=" + compare.getData().getWarnings());
        Assertions.assertNotNull(compare.getData().getReportPath());

        MigrationBusinessSignoffRequest signoff = new MigrationBusinessSignoffRequest();
        signoff.setApproved(true);
        signoff.setApprovedBy("staging-sim-ci");
        R<MigrationInventoryEntry> signed = templateController.recordMigrationSignoff(inventoryId, signoff);
        Assertions.assertTrue(signed.isSuccess(), signed.getMessage());
        Assertions.assertTrue(signed.getData().isBusinessSignoffApproved());

        R<TemplateV2DraftVO> promoted = templateController.promoteMigration(entity.getId());
        Assertions.assertTrue(promoted.isSuccess(), promoted.getMessage());

        MigrationInventoryEntry entry = migrationInventoryService.findById(inventoryId).orElseThrow();
        Assertions.assertTrue(entry.isV2DraftPresent());
        Assertions.assertEquals(compareClass, entry.getMigrationClass());
        Assertions.assertNotNull(entry.getLastCompareReportPath());

        TemplatePO persisted = templateRepository.findById(entity.getId()).orElseThrow();
        String yaml = persisted.getContentYaml();
        Assertions.assertTrue(yaml.contains("sources:"), () -> "expected V2 sources in:\n" + yaml);
        Assertions.assertTrue(yaml.contains("transform:"), () -> "expected V2 transform in:\n" + yaml);
        Assertions.assertNotNull(persisted.getContentJson());
        Assertions.assertTrue(persisted.getContentJson().contains("sources"));

        R<List<MigrationSignoffFamilyStatus>> status = templateController.migrationSignoffStatus();
        Assertions.assertTrue(status.isSuccess());
    }

    @TestConfiguration
    static class StagingSimulatedConfig {

        @Bean
        @Primary
        MigrationInventoryService testMigrationInventoryService() throws Exception {
            Path inventoryPath = Files.createTempFile("inventory-staging-sim", ".yaml");
            Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
            return new MigrationInventoryService(inventoryPath);
        }

        @Bean
        @Primary
        MigrationReportWriter testMigrationReportWriter() throws Exception {
            Path reportsDir = Files.createTempDirectory("migration-reports-staging-sim");
            return new MigrationReportWriter(reportsDir);
        }
    }
}
