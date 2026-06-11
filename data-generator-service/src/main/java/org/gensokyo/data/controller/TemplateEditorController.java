/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the operator console V2 template editor (forms + YAML advanced mode).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@RestController
@RequestMapping("/template/v2/editor")
@Validated
@RequiredArgsConstructor
public class TemplateEditorController {

    private final TemplateEditorService templateEditorService;

    /**
     * Returns a new unsaved V2 draft scaffold for the create wizard.
     *
     * @return empty draft payload
     */
    @PostMapping("/create")
    public R<TemplateEditorPayload> createDraft() {
        return R.ok("Draft created", templateEditorService.createEmptyDraft());
    }

    /**
     * Loads a persisted template for editing.
     *
     * @param templateId database template id
     * @return editor payload
     */
    @GetMapping("/{templateId}")
    public R<TemplateEditorPayload> load(@NotNull @PathVariable Long templateId) {
        try {
            return R.ok("Editor payload loaded", templateEditorService.loadForEditor(templateId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Persists a new template from a V2 draft.
     *
     * @param draft draft body
     * @return saved payload with id
     */
    @PostMapping
    public R<TemplateEditorPayload> createAndSave(@RequestBody TemplateV2DraftVO draft) {
        try {
            return R.ok("Template created", templateEditorService.createAndSave(draft));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Saves an existing template V2 draft.
     *
     * @param templateId target id
     * @param draft      draft body
     * @return saved payload
     */
    @PutMapping("/{templateId}")
    public R<TemplateEditorPayload> save(
            @NotNull @PathVariable Long templateId,
            @RequestBody TemplateV2DraftVO draft) {
        try {
            return R.ok("Template saved", templateEditorService.save(templateId, draft));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Archives a template (soft delete).
     *
     * @param templateId template id
     * @return success message
     */
    @PostMapping("/{templateId}/archive")
    public R<String> archive(@NotNull @PathVariable Long templateId) {
        try {
            templateEditorService.archive(templateId);
            return R.ok("Template archived");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * Restores an archived template.
     *
     * @param templateId template id
     * @return success message
     */
    @PostMapping("/{templateId}/restore")
    public R<String> restore(@NotNull @PathVariable Long templateId) {
        try {
            templateEditorService.restore(templateId);
            return R.ok("Template restored");
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
