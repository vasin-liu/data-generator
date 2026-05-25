/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.controller.TaskController;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.gensokyo.data.template.editor.TemplateEditorService;
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
        R<String> result = taskController.runById(resolvedId);
        if (!result.isSuccess()) {
            throw new IllegalArgumentException(result.getMessage());
        }
        String payload = result.getData() != null ? result.getData() : result.getMessage();
        return new RunStartResult(resolvedId, parseInstanceId(payload));
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
