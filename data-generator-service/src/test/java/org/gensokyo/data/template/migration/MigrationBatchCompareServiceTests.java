/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MigrationBatchCompareService} inventory selection and per-row handling.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@ExtendWith(MockitoExtension.class)
class MigrationBatchCompareServiceTests {

    @Mock
    private TemplateRepository repository;

    @Mock
    private MigrationCompareWorkflow compareWorkflow;

    @Test
    void runBatchSkipsCompatibilityOnlyAndComparesDbRows() throws Exception {
        Path inventoryPath = Files.createTempFile("inventory-batch", ".yaml");
        Files.writeString(inventoryPath, "version: 1\ntemplates: []\n");
        MigrationInventoryService inventoryService = new MigrationInventoryService(inventoryPath);

        MigrationInventoryEntry compat = new MigrationInventoryEntry();
        compat.setId("db-1");
        compat.setOrigin("database");
        compat.setDbTemplateId(1L);
        compat.setMigrationClass(MigrationClassification.COMPATIBILITY_ONLY);

        MigrationInventoryEntry runnable = new MigrationInventoryEntry();
        runnable.setId("db-2");
        runnable.setOrigin("database");
        runnable.setDbTemplateId(2L);
        runnable.setMigrationClass(MigrationClassification.UNCLASSIFIED);

        inventoryService.saveAll(List.of(compat, runnable));

        TemplatePO entity = new TemplatePO();
        entity.setId(2L);
        entity.setName("batch-target");
        entity.setContentYaml("""
                name: batch-target
                iterator:
                  type: constant
                  value: x
                output:
                  writers:
                    - type: console
                """);

        when(repository.findById(2L)).thenReturn(Optional.of(entity));

        MigrationComparisonReport report = new MigrationComparisonReport();
        report.setTemplateId(2L);
        report.setClassification(MigrationClassification.EXACT);
        report.setReportPath("docs/migration/reports/batch-stub.md");
        report.applyRecommendationFromClassification();
        when(compareWorkflow.compareAndPersist(eq(2L), eq(entity), any())).thenReturn(report);

        MigrationBatchCompareService service =
                new MigrationBatchCompareService(repository, inventoryService, compareWorkflow);

        MigrationBatchCompareOptions options = MigrationBatchCompareOptions.defaults();
        options.setRefreshInventoryFirst(false);

        MigrationBatchCompareResult result = service.runBatch(options);

        Assertions.assertEquals(1, result.getComparedCount());
        Assertions.assertEquals(1, result.getSkippedCount());
        Assertions.assertEquals(0, result.getFailedCount());
        Assertions.assertEquals(2, result.getItems().size());
    }
}
