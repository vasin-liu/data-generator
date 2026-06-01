/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.DraftPreviewRequest;
import org.gensokyo.data.api.console.dto.PreviewResultDto;
import org.gensokyo.data.api.console.dto.RunStartResultDto;
import org.gensokyo.data.api.console.dto.YamlApplyRequest;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.TemplateLifecycleService;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.TemplateV2ValidationResult;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.gensokyo.data.template.editor.TemplateEditorRunSupport;
import org.gensokyo.data.template.editor.TemplateEditorYamlSupport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Wizard actions: validate, preview, run-with-draft, YAML round-trip.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class ConsoleTemplateEditorActionsController {

    private final TemplateEditorService templateEditorService;
    private final TemplateEditorYamlSupport yamlSupport;
    private final TemplateV2ControlPlaneService controlPlaneService;
    private final TemplateEditorRunSupport templateEditorRunSupport;
    private final TemplateLifecycleService templateLifecycleService;

    /**
     * @param draft draft to validate
     * @return validation outcome
     */
    @PostMapping("/draft/validate")
    public R<TemplateV2ValidationResult> validateDraft(@RequestBody TemplateV2DraftVO draft) {
        return R.ok(controlPlaneService.validate(draft));
    }

    /**
     * Validates and publishes a persisted template (DRAFT → PUBLISHED).
     *
     * @param templateId template id
     * @return acknowledgement
     */
    @PostMapping("/{templateId}/publish")
    public R<String> publish(@NotNull @PathVariable Long templateId) {
        templateLifecycleService.publish(templateId);
        return R.ok("Template published");
    }

    /**
     * @param templateId optional persisted id (unused; draft body is authoritative)
     * @param draft      draft to validate
     * @return validation outcome
     */
    @PostMapping("/{templateId}/draft/validate")
    public R<TemplateV2ValidationResult> validateDraftForId(
            @NotNull @PathVariable Long templateId,
            @RequestBody TemplateV2DraftVO draft) {
        return validateDraft(draft);
    }

    /**
     * @param request draft and optional row cap
     * @return preview after create-or-save
     */
    @PostMapping("/draft/preview")
    public R<PreviewResultDto> previewDraft(@RequestBody DraftPreviewRequest request) {
        Integer maxRows = request.maxRows() != null
                ? request.maxRows()
                : TemplateEditorRunSupport.previewRowLimitFromDraft(request.draft());
        TemplateEditorRunSupport.PreviewResult result =
                templateEditorRunSupport.saveAndPreview(
                        null, request.draft(), maxRows, request.throughTransformIndex());
        return R.ok(new PreviewResultDto(result.templateId(), result.preview()));
    }

    /**
     * @param templateId persisted id or null path segment avoided — use draft/preview for new
     * @param request    draft body
     * @return preview result
     */
    @PostMapping("/{templateId}/draft/preview")
    public R<PreviewResultDto> previewDraftForId(
            @NotNull @PathVariable Long templateId,
            @RequestBody DraftPreviewRequest request) {
        Integer maxRows = request.maxRows() != null
                ? request.maxRows()
                : TemplateEditorRunSupport.previewRowLimitFromDraft(request.draft());
        TemplateEditorRunSupport.PreviewResult result =
                templateEditorRunSupport.saveAndPreview(
                        templateId, request.draft(), maxRows, request.throughTransformIndex());
        return R.ok(new PreviewResultDto(result.templateId(), result.preview()));
    }

    /**
     * @param draft draft to save and run
     * @return instance id
     */
    @PostMapping("/draft/run")
    public R<RunStartResultDto> runDraft(@RequestBody TemplateV2DraftVO draft) {
        TemplateEditorRunSupport.RunStartResult started = templateEditorRunSupport.saveAndRun(null, draft);
        return R.ok(new RunStartResultDto(started.templateId(), started.instanceId()));
    }

    /**
     * @param templateId persisted id
     * @param draft      current wizard draft
     * @return instance id
     */
    @PostMapping("/{templateId}/draft/run")
    public R<RunStartResultDto> runDraftForId(
            @NotNull @PathVariable Long templateId,
            @RequestBody TemplateV2DraftVO draft) {
        TemplateEditorRunSupport.RunStartResult started =
                templateEditorRunSupport.saveAndRun(templateId, draft);
        return R.ok(new RunStartResultDto(started.templateId(), started.instanceId()));
    }

    /**
     * @param templateId template id
     * @return YAML text for advanced mode
     */
    @GetMapping("/{templateId}/yaml")
    public R<String> exportYaml(@NotNull @PathVariable Long templateId) {
        TemplateEditorPayload payload = templateEditorService.loadForEditor(templateId);
        return R.ok(yamlSupport.toYaml(payload.draft()));
    }

    /**
     * @param draft in-memory wizard draft
     * @return YAML text reflecting current form state
     */
    @PostMapping("/draft/yaml")
    public R<String> exportDraftYaml(@RequestBody TemplateV2DraftVO draft) {
        return R.ok(yamlSupport.toYaml(draft));
    }

    /**
     * @param templateId persisted id (unused; draft body is authoritative)
     * @param draft      in-memory wizard draft
     * @return YAML text reflecting current form state
     */
    @PostMapping("/{templateId}/draft/yaml")
    public R<String> exportDraftYamlForId(
            @NotNull @PathVariable Long templateId,
            @RequestBody TemplateV2DraftVO draft) {
        return exportDraftYaml(draft);
    }

    /**
     * @param request YAML body
     * @return parsed draft without persisting (for new-template apply)
     */
    @PostMapping("/draft/yaml/parse")
    public R<TemplateV2DraftVO> parseDraftYaml(@RequestBody YamlApplyRequest request) {
        return R.ok(yamlSupport.parseYaml(request.yaml()));
    }

    /**
     * @param templateId template id
     * @param request    YAML body
     * @return updated editor payload
     */
    @PutMapping("/{templateId}/yaml")
    public R<TemplateEditorPayload> applyYaml(
            @NotNull @PathVariable Long templateId,
            @RequestBody YamlApplyRequest request) {
        TemplateV2DraftVO parsed = yamlSupport.parseYaml(request.yaml());
        TemplateEditorPayload saved = templateEditorService.save(templateId, parsed);
        return R.ok(saved);
    }
}
