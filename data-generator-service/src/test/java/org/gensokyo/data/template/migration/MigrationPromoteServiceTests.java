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
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
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
    void promoteRejectsCompatibilityOnlyClassification() throws Exception {
        MigrationInventoryService inventory = newInventoryService();
        MigrationPromoteService service = new MigrationPromoteService(
                repository,
                new MigrationDraftService(),
                inventory,
                new JacksonParser());

        TemplatePO entity = validV1PromotableTemplate(101L);
        when(repository.findById(101L)).thenReturn(Optional.of(entity));

        MigrationInventoryEntry entry = new MigrationInventoryEntry();
        entry.setId("db-101");
        entry.setOrigin("database");
        entry.setDbTemplateId(101L);
        entry.setMigrationClass(MigrationClassification.COMPATIBILITY_ONLY);
        inventory.saveAll(List.of(entry));

        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.promote(101L));
        Assertions.assertTrue(ex.getMessage().contains("COMPATIBILITY_ONLY"));
        verify(repository, never()).saveAndFlush(ArgumentMatchers.any());
    }

    @Test
    void promoteRejectsBlockedClassification() throws Exception {
        MigrationInventoryService inventory = newInventoryService();
        MigrationPromoteService service = new MigrationPromoteService(
                repository,
                new MigrationDraftService(),
                inventory,
                new JacksonParser());

        TemplatePO entity = validV1PromotableTemplate(102L);
        when(repository.findById(102L)).thenReturn(Optional.of(entity));

        MigrationInventoryEntry entry = new MigrationInventoryEntry();
        entry.setId("db-102");
        entry.setOrigin("database");
        entry.setDbTemplateId(102L);
        entry.setMigrationClass(MigrationClassification.BLOCKED);
        inventory.saveAll(List.of(entry));

        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.promote(102L));
        Assertions.assertTrue(ex.getMessage().contains("BLOCKED"));
        verify(repository, never()).saveAndFlush(ArgumentMatchers.any());
    }

    @Test
    void promoteRejectsWhenInventoryRowExistsWithoutBusinessSignoff() throws Exception {
        MigrationInventoryService inventory = newInventoryService();
        MigrationPromoteService service = new MigrationPromoteService(
                repository,
                new MigrationDraftService(),
                inventory,
                new JacksonParser());

        TemplatePO entity = validV1PromotableTemplate(103L);
        when(repository.findById(103L)).thenReturn(Optional.of(entity));

        MigrationInventoryEntry entry = new MigrationInventoryEntry();
        entry.setId("db-103");
        entry.setOrigin("database");
        entry.setDbTemplateId(103L);
        entry.setMigrationClass(MigrationClassification.EXACT);
        entry.setLastCompareReportPath("docs/migration/reports/sample.md");
        entry.setBusinessSignoffApproved(false);
        inventory.saveAll(List.of(entry));

        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.promote(103L));
        Assertions.assertTrue(ex.getMessage().toLowerCase().contains("sign-off")
                || ex.getMessage().toLowerCase().contains("signoff"));
        verify(repository, never()).saveAndFlush(ArgumentMatchers.any());
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
        MigrationInventoryEntry signedEntry = inventory.findById("db-100").orElseThrow();
        signedEntry.setBusinessSignoffApproved(true);
        inventory.saveAll(List.of(signedEntry));

        TemplateV2DraftVO draft = service.promote(100L);

        Assertions.assertNotNull(draft.getSources());
        MigrationInventoryEntry entry = inventory.findById("db-100").orElseThrow();
        Assertions.assertEquals(MigrationClassification.EXACT, entry.getMigrationClass());
        Assertions.assertTrue(entry.isV2DraftPresent());
        verify(repository).saveAndFlush(ArgumentMatchers.any(TemplatePO.class));
    }

    private static TemplatePO validV1PromotableTemplate(long id) {
        TemplatePO entity = new TemplatePO();
        entity.setId(id);
        entity.setContentYaml("""
                name: promote-candidate
                iterator:
                  type: number
                  from: 1
                  to: 2
                output:
                  writers:
                    - type: console
                """);
        return entity;
    }

    private static MigrationInventoryService newInventoryService() throws Exception {
        Path path = Files.createTempFile("inventory-promote", ".yaml");
        Files.writeString(path, "version: 1\ntemplates: []\n");
        return new MigrationInventoryService(path);
    }
}
