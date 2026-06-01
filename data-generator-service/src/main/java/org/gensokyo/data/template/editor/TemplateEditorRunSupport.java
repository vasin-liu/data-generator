/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.editor;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.controller.TaskController;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.TemplateV2PreviewDTO;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persists the editor draft and starts an async template run for the operator console.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@RequiredArgsConstructor
public class TemplateEditorRunSupport {

    private static final Pattern INSTANCE_ID = Pattern.compile("instanceId=(\\d+)");

    private final TemplateEditorService templateEditorService;
    private final TaskController taskController;
    private final TemplateV2ControlPlaneService controlPlaneService;

    /**
     * Result of save + async run submission.
     *
     * @param templateId persisted template id
     * @param instanceId snowflake run instance id
     */
    public record RunStartResult(long templateId, long instanceId) {
    }

    /**
     * Saves the draft (create or update), submits a run, and returns template + instance ids.
     *
     * @param templateId persisted id, or null to create
     * @param draft      current V2 draft from the wizard
     * @return ids for editor state and job detail navigation
     * @throws IllegalArgumentException when save or run fails
     */
    public RunStartResult saveAndRun(Long templateId, TemplateV2DraftVO draft) {
        long resolvedId;
        if (templateId == null) {
            TemplateEditorPayload created = templateEditorService.createAndSave(draft);
            resolvedId = created.templateId();
        } else {
            templateEditorService.save(templateId, draft);
            resolvedId = templateId;
        }
        R<String> result = taskController.runByIdAllowDraft(resolvedId);
        if (!result.isSuccess()) {
            throw new IllegalArgumentException(result.getMessage());
        }
        String payload = result.getData() != null ? result.getData() : result.getMessage();
        return new RunStartResult(resolvedId, parseInstanceId(payload));
    }

    /**
     * Starts a run for an already persisted template (no draft save).
     *
     * @param templateId persisted template id
     * @return template and instance ids
     * @throws IllegalArgumentException when run fails
     */
    public RunStartResult runExisting(long templateId) {
        R<String> result = taskController.runByIdAllowDraft(templateId);
        if (!result.isSuccess()) {
            throw new IllegalArgumentException(result.getMessage());
        }
        String payload = result.getData() != null ? result.getData() : result.getMessage();
        return new RunStartResult(templateId, parseInstanceId(payload));
    }

    /**
     * Persists the draft when needed, then runs a bounded in-memory preview.
     *
     * @param templateId persisted id, or null to create first
     * @param draft      current V2 draft
     * @param maxRows    optional row cap from execution policy
     * @return preview DTO and resolved template id
     * @throws IllegalArgumentException when save or preview fails
     */
    public PreviewResult saveAndPreview(Long templateId, TemplateV2DraftVO draft, Integer maxRows) {
        return saveAndPreview(templateId, draft, maxRows, null);
    }

    /**
     * Persists the draft when needed, then runs a bounded in-memory preview through an optional transform step.
     *
     * @param templateId            persisted id, or null to create first
     * @param draft                 current V2 draft
     * @param maxRows               optional row cap from execution policy
     * @param throughTransformIndex optional 0-based inclusive transformer index; when null, runs the full chain
     * @return preview DTO and resolved template id
     * @throws IllegalArgumentException when save or preview fails
     */
    public PreviewResult saveAndPreview(
            Long templateId,
            TemplateV2DraftVO draft,
            Integer maxRows,
            Integer throughTransformIndex) {
        long resolvedId;
        if (templateId == null) {
            TemplateEditorPayload created = templateEditorService.createAndSave(draft);
            resolvedId = created.templateId();
        } else {
            templateEditorService.save(templateId, draft);
            resolvedId = templateId;
        }
        TemplateV2PreviewDTO preview = controlPlaneService.preview(resolvedId, maxRows, throughTransformIndex);
        return new PreviewResult(resolvedId, preview);
    }

    /**
     * @param templateId resolved template id after save
     * @param preview    bounded preview result
     */
    public record PreviewResult(long templateId, TemplateV2PreviewDTO preview) {
    }

    /**
     * Reads preview row cap from draft execution policy when set.
     *
     * @param draft V2 draft
     * @return limit or null for service default
     */
    public static Integer previewRowLimitFromDraft(TemplateV2DraftVO draft) {
        if (draft == null || draft.getExecutionPolicy() == null) {
            return null;
        }
        ExecutionPolicyVO policy = draft.getExecutionPolicy();
        Integer limit = policy.getPreviewRowLimit();
        return limit != null && limit > 0 ? limit : null;
    }

    private static long parseInstanceId(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Run response missing instance id");
        }
        Matcher matcher = INSTANCE_ID.matcher(message);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        throw new IllegalArgumentException("Run response missing instanceId= in: " + message);
    }
}
