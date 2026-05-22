/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor.steps;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.TemplateV2ValidationResult;
import org.gensokyo.data.ui.template.editor.EditorStep;
import org.gensokyo.data.ui.template.editor.TemplateEditorModel;

import java.util.function.Consumer;

/**
 * Review step: validate draft and trigger save.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class ReviewStep implements EditorStep {

    private final VerticalLayout root = new VerticalLayout();
    private final Paragraph status = new Paragraph();
    private final TextArea validationOutput = new TextArea("Validation");
    private final Button validateButton = new Button("Validate");
    private final Button saveButton = new Button("Save");
    private final TemplateV2ControlPlaneService controlPlaneService;
    private final Consumer<TemplateEditorModel> saveHandler;

    /**
     * @param controlPlaneService validate API
     * @param saveHandler         invoked when Save is clicked (after apply)
     */
    public ReviewStep(TemplateV2ControlPlaneService controlPlaneService, Consumer<TemplateEditorModel> saveHandler) {
        this.controlPlaneService = controlPlaneService;
        this.saveHandler = saveHandler;
        validationOutput.setReadOnly(true);
        validationOutput.setMinHeight("200px");
        validationOutput.setWidthFull();
        validateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        saveButton.addClickListener(e -> {
            if (currentModel != null && currentModel.isSaveAllowed()) {
                saveHandler.accept(currentModel);
            }
        });
        validateButton.addClickListener(e -> runValidate());
        HorizontalLayout actions = new HorizontalLayout(validateButton, saveButton);
        root.add(status, actions, validationOutput);
    }

    private TemplateEditorModel currentModel;
    private Runnable applyAllSteps;

    /**
     * Wires step apply callback before validate/save.
     *
     * @param applyAllSteps flushes wizard fields into the model
     */
    public void setApplyAllSteps(Runnable applyAllSteps) {
        this.applyAllSteps = applyAllSteps;
    }

    @Override
    public Component getView() {
        return root;
    }

    @Override
    public void refreshFromModel(TemplateEditorModel model) {
        this.currentModel = model;
        status.setText("Kind: " + model.getKind()
                + (model.getTemplateId() != null ? " | id=" + model.getTemplateId() : " | new")
                + (model.isArchived() ? " | ARCHIVED" : ""));
        saveButton.setEnabled(model.isSaveAllowed());
        validateButton.setEnabled(model.isSaveAllowed());
    }

    @Override
    public void applyToModel(TemplateEditorModel model) {
        // Review does not mutate draft fields
    }

    private void runValidate() {
        if (currentModel == null || !currentModel.isSaveAllowed()) {
            return;
        }
        if (applyAllSteps != null) {
            applyAllSteps.run();
        }
        try {
            TemplateV2ValidationResult result = controlPlaneService.validate(currentModel.getDraft());
            validationOutput.setValue(String.join(System.lineSeparator(), result.getErrors())
                    + System.lineSeparator()
                    + String.join(System.lineSeparator(), result.getWarnings()));
            if (result.isValid()) {
                Notification.show("Validation passed");
            } else {
                Notification.show("Validation failed");
            }
        } catch (Exception ex) {
            validationOutput.setValue(ex.getMessage());
            Notification.show("Validation error: " + ex.getMessage());
        }
    }
}
