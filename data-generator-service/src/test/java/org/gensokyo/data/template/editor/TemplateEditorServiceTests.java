/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.editor;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

/**
 * Tests for {@link TemplateEditorService} archive and V2 round-trip.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateEditorServiceTests {

    @Autowired
    private TemplateEditorService templateEditorService;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void createAndSaveRoundTrip() {
        TemplateEditorPayload created = templateEditorService.createAndSave(
                templateEditorService.createEmptyDraft().draft());
        Assertions.assertNotNull(created.templateId());
        Assertions.assertEquals(TemplateDefinitionKind.V2, created.kind());

        TemplateEditorPayload loaded = templateEditorService.loadForEditor(created.templateId());
        Assertions.assertEquals("new-template", loaded.draft().getName());
        Assertions.assertFalse(loaded.archived());
    }

    @Test
    void archiveExcludesFromActiveList() {
        TemplateEditorPayload created = templateEditorService.createAndSave(
                templateEditorService.createEmptyDraft().draft());
        templateEditorService.archive(created.templateId());

        Assertions.assertTrue(templateRepository.findByArchivedTrue().stream()
                .anyMatch(t -> t.getId().equals(created.templateId())));
        Assertions.assertTrue(templateRepository.findByArchivedFalse().stream()
                .noneMatch(t -> t.getId().equals(created.templateId())));
    }

    @Test
    void saveRejectsArchivedTemplate() {
        TemplateEditorPayload created = templateEditorService.createAndSave(
                templateEditorService.createEmptyDraft().draft());
        templateEditorService.archive(created.templateId());

        TemplateV2DraftVO draft = templateEditorService.loadForEditor(created.templateId()).draft();
        draft.setName("renamed");
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> templateEditorService.save(created.templateId(), draft));
    }

    @Test
    void loadForEditor_rejectsV1Template() throws Exception {
        String yaml = new ClassPathResource("migration/regression/v1-iterator-simple.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        TemplatePO entity = new TemplatePO();
        entity.setId(88001L);
        entity.setName("legacy-v1");
        entity.setContentYaml(yaml);
        entity.setArchived(false);
        TemplatePO saved = templateRepository.saveAndFlush(entity);
        Long templateId = saved.getId();

        Assertions.assertEquals(TemplateDefinitionKind.V1, templateEditorService.detectDefinitionKind(saved));
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> templateEditorService.loadForEditor(templateId));
    }

    @Test
    void restoreAllowsSaveAgain() {
        TemplateEditorPayload created = templateEditorService.createAndSave(
                templateEditorService.createEmptyDraft().draft());
        templateEditorService.archive(created.templateId());
        templateEditorService.restore(created.templateId());

        TemplateV2DraftVO draft = templateEditorService.loadForEditor(created.templateId()).draft();
        draft.setName("restored-name");
        TemplateEditorPayload saved = templateEditorService.save(created.templateId(), draft);
        Assertions.assertEquals("restored-name", saved.draft().getName());
    }
}
