/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.api.console.dto.RunStartResultDto;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.gensokyo.data.template.editor.TemplateEditorRunSupport;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Template editor CRUD for the React wizard (delegates to {@link TemplateEditorService}).
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/templates")
@Validated
@RequiredArgsConstructor
public class ConsoleTemplateEditorController {

    private final TemplateEditorService templateEditorService;
    private final TemplateEditorRunSupport templateEditorRunSupport;

    /**
     * @return empty draft scaffold for create wizard
     */
    @GetMapping("/scaffold")
    public R<TemplateEditorPayload> scaffold() {
        return R.ok("Draft created", templateEditorService.createEmptyDraft());
    }

    /**
     * @param templateId persisted id
     * @return editor payload
     */
    @GetMapping("/{templateId}")
    public R<TemplateEditorPayload> load(@NotNull @PathVariable Long templateId) {
        return R.ok("Editor payload loaded", templateEditorService.loadForEditor(templateId));
    }

    /**
     * @param draft first save for a new template
     * @return saved payload with id
     */
    @PostMapping
    public R<TemplateEditorPayload> createAndSave(@RequestBody TemplateV2DraftVO draft) {
        return R.ok("Template created", templateEditorService.createAndSave(draft));
    }

    /**
     * @param templateId target id
     * @param draft      body
     * @return saved payload
     */
    @PutMapping("/{templateId}")
    public R<TemplateEditorPayload> save(
            @NotNull @PathVariable Long templateId,
            @RequestBody TemplateV2DraftVO draft) {
        return R.ok("Template saved", templateEditorService.save(templateId, draft));
    }

    /**
     * @param templateId template id
     * @return success message
     */
    @PostMapping("/{templateId}/archive")
    public R<String> archive(@NotNull @PathVariable Long templateId) {
        templateEditorService.archive(templateId);
        return R.ok("Template archived");
    }

    /**
     * @param templateId template id
     * @return success message
     */
    @PostMapping("/{templateId}/restore")
    public R<String> restore(@NotNull @PathVariable Long templateId) {
        templateEditorService.restore(templateId);
        return R.ok("Template restored");
    }

    /**
     * @param templateId persisted template id
     * @return snowflake instance id for job detail navigation
     */
    @PostMapping("/{templateId}/run")
    public R<RunStartResultDto> run(@NotNull @PathVariable Long templateId) {
        TemplateEditorRunSupport.RunStartResult started = templateEditorRunSupport.runExisting(templateId);
        return R.ok(new RunStartResultDto(started.templateId(), started.instanceId()));
    }
}
