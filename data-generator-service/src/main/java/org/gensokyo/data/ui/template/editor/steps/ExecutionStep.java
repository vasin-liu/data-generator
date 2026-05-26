/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor.steps;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.ui.VaadinFieldSupport;
import org.gensokyo.data.ui.i18n.ConsoleI18n;
import org.gensokyo.data.ui.template.editor.EditorStep;
import org.gensokyo.data.ui.template.editor.TemplateEditorModel;

/**
 * Execution policy step (mode and chunk sizes).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class ExecutionStep implements EditorStep {

    private final FormLayout form = new FormLayout();
    private final ComboBox<String> mode = new ComboBox<>("Execution mode");
    private final IntegerField sourceChunkSize = new IntegerField("Source chunk size");
    private final IntegerField sinkBatchSize = new IntegerField("Sink batch size");
    private final IntegerField previewRowLimit = new IntegerField("Preview row limit");

    /**
     * Creates execution policy fields.
     */
    public ExecutionStep() {
        mode.setItems("BOUNDED", "CHUNKED");
        form.add(mode, sourceChunkSize, sinkBatchSize, previewRowLimit);
        applyI18n();
    }

    private void applyI18n() {
        mode.setLabel(ConsoleI18n.tr("execution.mode"));
        sourceChunkSize.setLabel(ConsoleI18n.tr("execution.sourceChunk"));
        sinkBatchSize.setLabel(ConsoleI18n.tr("execution.sinkBatch"));
        previewRowLimit.setLabel(ConsoleI18n.tr("execution.previewLimit"));
    }

    @Override
    public Component getView() {
        return form;
    }

    @Override
    public void refreshFromModel(TemplateEditorModel model) {
        ExecutionPolicyVO policy = model.getDraft().getExecutionPolicy();
        if (policy == null) {
            mode.clear();
            sourceChunkSize.clear();
            sinkBatchSize.clear();
            previewRowLimit.clear();
        } else {
            VaadinFieldSupport.setCombo(mode, policy.getMode());
            VaadinFieldSupport.setInteger(sourceChunkSize, policy.getSourceChunkSize());
            VaadinFieldSupport.setInteger(sinkBatchSize, policy.getSinkBatchSize());
            VaadinFieldSupport.setInteger(previewRowLimit, policy.getPreviewRowLimit());
        }
        boolean enabled = model.isSaveAllowed();
        mode.setReadOnly(!enabled);
        sourceChunkSize.setReadOnly(!enabled);
        sinkBatchSize.setReadOnly(!enabled);
        previewRowLimit.setReadOnly(!enabled);
    }

    @Override
    public void applyToModel(TemplateEditorModel model) {
        if (!model.isSaveAllowed()) {
            return;
        }
        TemplateV2DraftVO draft = model.getDraft();
        ExecutionPolicyVO policy = draft.getExecutionPolicy();
        if (policy == null) {
            policy = new ExecutionPolicyVO();
            draft.setExecutionPolicy(policy);
        }
        policy.setMode(mode.getValue());
        policy.setSourceChunkSize(sourceChunkSize.getValue());
        policy.setSinkBatchSize(sinkBatchSize.getValue());
        policy.setPreviewRowLimit(previewRowLimit.getValue());
    }
}
