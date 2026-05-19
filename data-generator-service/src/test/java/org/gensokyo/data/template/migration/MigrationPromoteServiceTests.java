/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MigrationPromoteService}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@ExtendWith(MockitoExtension.class)
class MigrationPromoteServiceTests {

    @Mock
    private TemplateRepository repository;

    @Test
    void promoteRequiresValidatedV2Draft() throws Exception {
        MigrationInventoryService inventory = newInventoryService();
        MigrationPromoteService service = new MigrationPromoteService(
                repository,
                new MigrationDraftService(),
                inventory,
                new JacksonParser());

        TemplatePO entity = new TemplatePO();
        entity.setId(99L);
        entity.setContentYaml("""
                name: bad
                iterator:
                  type: excel
                  path: missing.xlsx
                output:
                  writers:
                    - type: console
                """);
        when(repository.findById(99L)).thenReturn(Optional.of(entity));

        Assertions.assertThrows(IllegalArgumentException.class, () -> service.promote(99L));
    }

    @Test
    void promoteUpdatesInventoryClassificationFromLastCompare() throws Exception {
        MigrationInventoryService inventory = newInventoryService();
        MigrationPromoteService service = new MigrationPromoteService(
                repository,
                new MigrationDraftService(),
                inventory,
                new JacksonParser());

        TemplatePO entity = new TemplatePO();
        entity.setId(100L);
        entity.setContentYaml("""
                name: promote-me
                iterator:
                  type: number
                  from: 1
                  to: 2
                output:
                  writers:
                    - type: console
                """);
        when(repository.findById(100L)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        MigrationComparisonReport report = new MigrationComparisonReport();
        report.setTemplateId(100L);
        report.setClassification(MigrationClassification.EXACT);
        report.applyRecommendationFromClassification();
        inventory.updateCompareResult(100L, report, "docs/migration/reports/sample-promote.md");

        TemplateV2DraftVO draft = service.promote(100L);

        Assertions.assertNotNull(draft.getSources());
        MigrationInventoryEntry entry = inventory.findById("db-100").orElseThrow();
        Assertions.assertEquals(MigrationClassification.EXACT, entry.getMigrationClass());
        Assertions.assertTrue(entry.isV2DraftPresent());
        verify(repository).saveAndFlush(ArgumentMatchers.any(TemplatePO.class));
    }

    private static MigrationInventoryService newInventoryService() throws Exception {
        Path path = Files.createTempFile("inventory-promote", ".yaml");
        Files.writeString(path, "version: 1\ntemplates: []\n");
        return new MigrationInventoryService(path);
    }
}
