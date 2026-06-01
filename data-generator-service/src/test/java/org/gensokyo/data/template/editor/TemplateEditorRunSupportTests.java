/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.editor;

import org.gensokyo.data.controller.TaskController;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.TemplateV2PreviewDTO;
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

    @Mock
    private TemplateV2ControlPlaneService controlPlaneService;

    @InjectMocks
    private TemplateEditorRunSupport runSupport;

    @Test
    void saveAndRunCreatesTemplateWhenIdNull() {
        TemplateV2DraftVO draft = new TemplateV2DraftVO();
        when(templateEditorService.createAndSave(draft))
                .thenReturn(new TemplateEditorPayload(42L, null, draft, null, false));
        when(taskController.runByIdAllowDraft(42L))
                .thenReturn(R.ok("Template 't' started. templateId=42, instanceId=9001"));

        TemplateEditorRunSupport.RunStartResult result = runSupport.saveAndRun(null, draft);

        Assertions.assertEquals(42L, result.templateId());
        Assertions.assertEquals(9001L, result.instanceId());
        verify(templateEditorService).createAndSave(draft);
        verify(taskController).runByIdAllowDraft(42L);
    }

    @Test
    void saveAndRunUpdatesExistingTemplate() {
        TemplateV2DraftVO draft = new TemplateV2DraftVO();
        when(taskController.runByIdAllowDraft(7L))
                .thenReturn(R.ok("Template 't' started. templateId=7, instanceId=7000"));

        TemplateEditorRunSupport.RunStartResult result = runSupport.saveAndRun(7L, draft);

        Assertions.assertEquals(7L, result.templateId());
        Assertions.assertEquals(7000L, result.instanceId());
        verify(templateEditorService).save(eq(7L), any());
    }

    @Test
    void runExistingUsesPersistedTemplate() {
        when(taskController.runByIdAllowDraft(9L))
                .thenReturn(R.ok("Template 't' started. templateId=9, instanceId=9009"));

        TemplateEditorRunSupport.RunStartResult result = runSupport.runExisting(9L);

        Assertions.assertEquals(9L, result.templateId());
        Assertions.assertEquals(9009L, result.instanceId());
    }

    @Test
    void saveAndPreviewPersistsThenPreviews() {
        TemplateV2DraftVO draft = new TemplateV2DraftVO();
        TemplateV2PreviewDTO preview = new TemplateV2PreviewDTO();
        when(templateEditorService.save(3L, draft))
                .thenReturn(new TemplateEditorPayload(3L, null, draft, null, false));
        when(controlPlaneService.preview(3L, 5, null)).thenReturn(preview);

        TemplateEditorRunSupport.PreviewResult result = runSupport.saveAndPreview(3L, draft, 5);

        Assertions.assertEquals(3L, result.templateId());
        Assertions.assertSame(preview, result.preview());
        verify(controlPlaneService).preview(3L, 5, null);
    }
}
