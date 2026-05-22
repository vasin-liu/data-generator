/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor;

import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.editor.TemplateEditorPayload;

/**
 * Mutable editor state shared across wizard steps and YAML advanced panel.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class TemplateEditorModel {

    private Long templateId;
    private TemplateDefinitionKind kind = TemplateDefinitionKind.V2;
    private TemplateV2DraftVO draft;
    private String v1Yaml;
    private boolean archived;
    private boolean readOnlyForm;

    /**
     * Builds a model from a service payload.
     *
     * @param payload editor payload
     * @return model instance
     */
    public static TemplateEditorModel fromPayload(TemplateEditorPayload payload) {
        TemplateEditorModel model = new TemplateEditorModel();
        model.templateId = payload.templateId();
        model.kind = payload.kind();
        model.draft = payload.draft();
        model.v1Yaml = payload.v1Yaml();
        model.archived = payload.archived();
        model.readOnlyForm = payload.kind() == TemplateDefinitionKind.V1;
        return model;
    }

    /**
     * @return persisted template id, or null when creating
     */
    public Long getTemplateId() {
        return templateId;
    }

    /**
     * @param templateId persisted id
     */
    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    /**
     * @return detected definition kind
     */
    public TemplateDefinitionKind getKind() {
        return kind;
    }

    /**
     * @param kind definition kind
     */
    public void setKind(TemplateDefinitionKind kind) {
        this.kind = kind;
    }

    /**
     * @return editable V2 draft
     */
    public TemplateV2DraftVO getDraft() {
        return draft;
    }

    /**
     * @param draft V2 draft
     */
    public void setDraft(TemplateV2DraftVO draft) {
        this.draft = draft;
    }

    /**
     * @return V1 YAML when legacy template
     */
    public String getV1Yaml() {
        return v1Yaml;
    }

    /**
     * @return whether template is archived
     */
    public boolean isArchived() {
        return archived;
    }

    /**
     * @param archived archive flag
     */
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    /**
     * @return true when structured form must not edit (V1 legacy)
     */
    public boolean isReadOnlyForm() {
        return readOnlyForm;
    }

    /**
     * @return true when save is allowed from the UI
     */
    public boolean isSaveAllowed() {
        return !readOnlyForm && !archived;
    }
}
