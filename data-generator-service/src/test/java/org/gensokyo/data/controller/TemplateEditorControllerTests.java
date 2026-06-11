/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {@link TemplateEditorController}.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateEditorControllerTests {

    @Autowired
    private TemplateEditorController templateEditorController;

    @Autowired
    private org.gensokyo.data.repository.TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void createDraftReturnsScaffold() {
        R<TemplateEditorPayload> result = templateEditorController.createDraft();
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertNull(result.getData().templateId());
    }

    @Test
    void createAndSaveThenLoad() {
        R<TemplateEditorPayload> draft = templateEditorController.createDraft();
        R<TemplateEditorPayload> created = templateEditorController.createAndSave(draft.getData().draft());
        Assertions.assertTrue(created.isSuccess());

        R<TemplateEditorPayload> loaded = templateEditorController.load(created.getData().templateId());
        Assertions.assertTrue(loaded.isSuccess());
        Assertions.assertEquals(created.getData().templateId(), loaded.getData().templateId());
    }

    @Test
    void archiveAndRestore() {
        R<TemplateEditorPayload> created = templateEditorController.createAndSave(
                templateEditorController.createDraft().getData().draft());
        Long id = created.getData().templateId();

        R<String> archived = templateEditorController.archive(id);
        Assertions.assertTrue(archived.isSuccess());

        R<String> restored = templateEditorController.restore(id);
        Assertions.assertTrue(restored.isSuccess());
    }
}
