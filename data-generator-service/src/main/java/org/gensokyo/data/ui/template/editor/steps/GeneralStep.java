/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor.steps;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.ui.template.editor.EditorStep;
import org.gensokyo.data.ui.template.editor.TemplateEditorModel;

/**
 * General template metadata step (name and generator batch size).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class GeneralStep implements EditorStep {

    private final FormLayout form = new FormLayout();
    private final TextField nameField = new TextField("Template name");
    private final IntegerField batchSizeField = new IntegerField("Generator batch size");
    private final Binder<TemplateV2DraftVO> binder = new Binder<>(TemplateV2DraftVO.class);

    /**
     * Creates the step and binds fields.
     */
    public GeneralStep() {
        batchSizeField.setMin(1);
        batchSizeField.setStepButtonsVisible(true);
        form.add(nameField, batchSizeField);
        binder.forField(nameField).bind(TemplateV2DraftVO::getName, TemplateV2DraftVO::setName);
    }

    @Override
    public Component getView() {
        return form;
    }

    @Override
    public void refreshFromModel(TemplateEditorModel model) {
        TemplateV2DraftVO draft = model.getDraft();
        if (draft.getGenerator() == null) {
            draft.setGenerator(new GeneratorVO());
        }
        binder.setBean(draft);
        GeneratorVO generator = draft.getGenerator();
        batchSizeField.setValue(generator.getBatchSize() > 0 ? generator.getBatchSize() : 100);
        boolean enabled = model.isSaveAllowed();
        nameField.setReadOnly(!enabled);
        batchSizeField.setReadOnly(!enabled);
    }

    @Override
    public void applyToModel(TemplateEditorModel model) {
        if (!model.isSaveAllowed()) {
            return;
        }
        TemplateV2DraftVO draft = model.getDraft();
        binder.writeBeanIfValid(draft);
        if (draft.getGenerator() == null) {
            draft.setGenerator(new GeneratorVO());
        }
        Integer batch = batchSizeField.getValue();
        if (batch != null && batch > 0) {
            draft.getGenerator().setBatchSize(batch);
        }
    }
}
