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
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.TemplateV2ValidationResult;
import org.gensokyo.data.ui.job.JobDetailView;
import org.gensokyo.data.ui.template.editor.EditorStep;
import org.gensokyo.data.ui.template.editor.TemplateEditorModel;
import org.gensokyo.data.ui.template.editor.TemplateEditorRunSupport;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

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
    private final Button runButton = new Button("Run");
    private final TemplateV2ControlPlaneService controlPlaneService;
    private final TemplateEditorRunSupport runSupport;
    private final Consumer<TemplateEditorModel> saveHandler;
    private final LongConsumer onTemplateIdAssigned;

    /**
     * @param controlPlaneService validate API
     * @param runSupport          save draft and start task run
     * @param saveHandler         invoked when Save is clicked (after apply)
     * @param onTemplateIdAssigned called when run creates a new persisted template id
     */
    public ReviewStep(
            TemplateV2ControlPlaneService controlPlaneService,
            TemplateEditorRunSupport runSupport,
            Consumer<TemplateEditorModel> saveHandler,
            LongConsumer onTemplateIdAssigned) {
        this.controlPlaneService = controlPlaneService;
        this.runSupport = runSupport;
        this.saveHandler = saveHandler;
        this.onTemplateIdAssigned = onTemplateIdAssigned;
        validationOutput.setReadOnly(true);
        validationOutput.setMinHeight("200px");
        validationOutput.setWidthFull();
        validateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        runButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        saveButton.addClickListener(e -> {
            if (currentModel != null && currentModel.isSaveAllowed()) {
                saveHandler.accept(currentModel);
            }
        });
        runButton.addClickListener(e -> runTemplate());
        validateButton.addClickListener(e -> runValidate());
        HorizontalLayout actions = new HorizontalLayout(validateButton, saveButton, runButton);
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
        boolean canRun = model.isSaveAllowed() && model.getKind() != TemplateDefinitionKind.V1;
        saveButton.setEnabled(model.isSaveAllowed());
        validateButton.setEnabled(model.isSaveAllowed());
        runButton.setEnabled(canRun);
        runButton.setTooltipText(canRun ? "Save draft and start run" : "V1 templates: use Migration tab to promote first");
    }

    @Override
    public void applyToModel(TemplateEditorModel model) {
        // Review does not mutate draft fields
    }

    private void runTemplate() {
        if (currentModel == null || !currentModel.isSaveAllowed()) {
            return;
        }
        if (currentModel.getKind() == TemplateDefinitionKind.V1) {
            Notification.show("Promote to V2 before running from the editor");
            return;
        }
        if (applyAllSteps != null) {
            applyAllSteps.run();
        }
        try {
            TemplateEditorRunSupport.RunStartResult started =
                    runSupport.saveAndRun(currentModel.getTemplateId(), currentModel.getDraft());
            if (currentModel.getTemplateId() == null) {
                currentModel.setTemplateId(started.templateId());
                onTemplateIdAssigned.accept(started.templateId());
            }
            Notification.show("Run started — opening job detail");
            runButton.getUI().ifPresent(
                    ui -> ui.navigate(JobDetailView.class, String.valueOf(started.instanceId())));
        } catch (IllegalArgumentException ex) {
            Notification.show("Run failed: " + ex.getMessage());
        }
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
