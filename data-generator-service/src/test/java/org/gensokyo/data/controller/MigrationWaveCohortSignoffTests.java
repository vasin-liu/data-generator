/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.migration.MigrationBusinessSignoffRequest;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventoryService;
import org.gensokyo.data.template.migration.MigrationSignoffFamilyStatus;
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
 * P3 wave cohort: business sign-off for {@code synthetic} (W1) and {@code multi_source} (W2) inventory rows.
 * Cohort ids align with {@code docs/migration/scenario-inventory.yaml}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(MigrationWaveCohortSignoffTests.SignoffTestConfig.class)
class MigrationWaveCohortSignoffTests {

    @Autowired
    private TemplateController templateController;

    @Autowired
    private MigrationInventoryService migrationInventoryService;

    @Test
    void waveOneSyntheticFamilySignoffCompletes() {
        List<MigrationInventoryEntry> wave1 = waveOneSyntheticCohort();
        migrationInventoryService.saveAll(wave1);
        signOffAllReady(wave1);

        R<List<MigrationSignoffFamilyStatus>> status = templateController.migrationSignoffStatus();
        Assertions.assertTrue(status.isSuccess());
        MigrationSignoffFamilyStatus synthetic = findFamily(status.getData(), "synthetic");
        Assertions.assertTrue(synthetic.isFamilySignoffComplete(), () -> "pending=" + synthetic.getPendingSignoff());
        Assertions.assertEquals(0, synthetic.getPendingSignoff());
    }

    @Test
    void waveTwoMultiSourceFamilySignoffCompletes() {
        List<MigrationInventoryEntry> wave2 = waveTwoMultiSourceCohort();
        migrationInventoryService.saveAll(wave2);
        signOffAllReady(wave2);

        R<List<MigrationSignoffFamilyStatus>> status = templateController.migrationSignoffStatus();
        Assertions.assertTrue(status.isSuccess());
        MigrationSignoffFamilyStatus multiSource = findFamily(status.getData(), "multi_source");
        Assertions.assertTrue(multiSource.isFamilySignoffComplete());
        Assertions.assertEquals(0, multiSource.getPendingSignoff());
    }

    /**
     * Wave 1 synthetic cohort (matches scenario-inventory.yaml).
     *
     * @return inventory rows ready for sign-off
     */
    private static List<MigrationInventoryEntry> waveOneSyntheticCohort() {
        return List.of(
                cohortRow(
                        "regression-v1-constant-five-rows",
                        "synthetic",
                        MigrationClassification.EXACT,
                        1,
                        "docs/migration/reports/sample-regression-v1-constant-five-rows.md"),
                cohortRow(
                        "regression-v1-iterator-simple",
                        "synthetic",
                        MigrationClassification.ADAPTED,
                        1,
                        "docs/migration/reports/sample-regression-v1-iterator-simple.md"),
                cohortRow(
                        "wave1-synthetic-number-1to5",
                        "synthetic",
                        MigrationClassification.EXACT,
                        1,
                        "docs/migration/reports/sample-regression-v1-constant-five-rows.md"),
                cohortRow(
                        "wave1-synthetic-iterator-draft",
                        "synthetic",
                        MigrationClassification.ADAPTED,
                        1,
                        "docs/migration/reports/sample-regression-v1-iterator-simple.md"));
    }

    /**
     * Wave 2 multi-source cohort (matches scenario-inventory.yaml).
     *
     * @return inventory rows ready for sign-off
     */
    private static List<MigrationInventoryEntry> waveTwoMultiSourceCohort() {
        return List.of(
                cohortRow(
                        "regression-v1-query-lookup",
                        "multi_source",
                        MigrationClassification.ADAPTED,
                        2,
                        "docs/migration/reports/sample-regression-v1-query-lookup.md"),
                cohortRow(
                        "wave2-jdbc-single-reader",
                        "multi_source",
                        MigrationClassification.ADAPTED,
                        2,
                        "docs/migration/reports/sample-regression-v1-query-lookup.md"),
                cohortRow(
                        "wave2-jdbc-chunked-policy",
                        "multi_source",
                        MigrationClassification.ADAPTED,
                        2,
                        "docs/migration/reports/sample-regression-v1-query-lookup.md"));
    }

    private static MigrationInventoryEntry cohortRow(
            String id,
            String family,
            MigrationClassification migrationClass,
            int wave,
            String reportPath) {
        MigrationInventoryEntry entry = new MigrationInventoryEntry();
        entry.setId(id);
        entry.setName(id);
        entry.setOrigin("repository");
        entry.setScenarioFamily(family);
        entry.setMigrationClass(migrationClass);
        entry.setWave(wave);
        entry.setLastCompareReportPath(reportPath);
        entry.setV2DraftPresent(true);
        return entry;
    }

    private void signOffAllReady(List<MigrationInventoryEntry> entries) {
        MigrationBusinessSignoffRequest request = new MigrationBusinessSignoffRequest();
        request.setApproved(true);
        request.setApprovedBy("automated-p3-cohort-test");
        for (MigrationInventoryEntry entry : entries) {
            R<MigrationInventoryEntry> result = templateController.recordMigrationSignoff(entry.getId(), request);
            Assertions.assertTrue(result.isSuccess(), () -> entry.getId() + ": " + result.getMessage());
            Assertions.assertTrue(result.getData().isBusinessSignoffApproved());
        }
    }

    private static MigrationSignoffFamilyStatus findFamily(
            List<MigrationSignoffFamilyStatus> statuses,
            String family) {
        return statuses.stream()
                .filter(s -> family.equals(s.getScenarioFamily()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Family not found: " + family));
    }

    @TestConfiguration
    static class SignoffTestConfig {

        @Bean
        @Primary
        MigrationInventoryService testMigrationInventoryService() throws Exception {
            Path inventoryPath = Files.createTempFile("inventory-wave-signoff", ".yaml");
            Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
            return new MigrationInventoryService(inventoryPath);
        }
    }
}
