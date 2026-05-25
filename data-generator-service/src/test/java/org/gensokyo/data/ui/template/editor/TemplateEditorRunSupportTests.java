/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor;

import org.gensokyo.data.controller.TaskController;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemplateEditorRunSupport}.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@ExtendWith(MockitoExtension.class)
class TemplateEditorRunSupportTests {

    @Mock
    private TemplateEditorService templateEditorService;

    @Mock
    private TaskController taskController;

    @InjectMocks
    private TemplateEditorRunSupport runSupport;

    @Test
    void saveAndRunCreatesTemplateWhenIdNull() {
        TemplateV2DraftVO draft = new TemplateV2DraftVO();
        when(templateEditorService.createAndSave(draft))
                .thenReturn(new TemplateEditorPayload(42L, null, draft, null, false));
        when(taskController.runById(42L))
                .thenReturn(R.ok("Template 't' started. templateId=42, instanceId=9001"));

        TemplateEditorRunSupport.RunStartResult result = runSupport.saveAndRun(null, draft);

        Assertions.assertEquals(42L, result.templateId());
        Assertions.assertEquals(9001L, result.instanceId());
        verify(templateEditorService).createAndSave(draft);
        verify(taskController).runById(42L);
    }

    @Test
    void saveAndRunUpdatesExistingTemplate() {
        TemplateV2DraftVO draft = new TemplateV2DraftVO();
        when(taskController.runById(7L))
                .thenReturn(R.ok("Template 't' started. templateId=7, instanceId=7000"));

        TemplateEditorRunSupport.RunStartResult result = runSupport.saveAndRun(7L, draft);

        Assertions.assertEquals(7L, result.templateId());
        Assertions.assertEquals(7000L, result.instanceId());
        verify(templateEditorService).save(eq(7L), any());
    }
}
