/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.template.TemplateListView;
import org.gensokyo.data.ui.template.editor.steps.ExecutionStep;
import org.gensokyo.data.ui.template.editor.steps.GeneralStep;
import org.gensokyo.data.ui.template.editor.steps.ReviewStep;
import org.gensokyo.data.ui.template.editor.steps.SinksStep;
import org.gensokyo.data.ui.template.editor.steps.SourcesStep;
import org.gensokyo.data.ui.template.editor.steps.TransformStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * V2 template editor with wizard steps and YAML advanced mode.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@Route(value = "template/editor/:templateId?", layout = MainLayout.class)
@PageTitle("Template editor | Data Generator")
public class TemplateEditorView extends VerticalLayout implements HasUrlParameter<String> {

    private final TemplateEditorService templateEditorService;
    private final TemplateEditorYamlSupport yamlSupport;
    private final TemplateV2ControlPlaneService controlPlaneService;
    private final DynamicRoutingDataSource dynamicRoutingDataSource;

    private final VerticalLayout stepContent = new VerticalLayout();
    private final Div yamlPanel = new Div();
    private final TextArea yamlArea = new TextArea("YAML advanced");
    private final Checkbox yamlMode = new Checkbox("YAML advanced mode");
    private final Map<Tab, EditorStep> steps = new LinkedHashMap<>();
    private TemplateEditorModel model;

    /**
     * Spring-injected editor view.
     *
     * @param templateEditorService   persistence
     * @param yamlSupport             YAML parse/dump
     * @param controlPlaneService     validate
     * @param dynamicRoutingDataSource optional JDBC name list
     */
    @Autowired
    public TemplateEditorView(
            TemplateEditorService templateEditorService,
            TemplateEditorYamlSupport yamlSupport,
            TemplateV2ControlPlaneService controlPlaneService,
            @Autowired(required = false) DynamicRoutingDataSource dynamicRoutingDataSource) {
        this.templateEditorService = templateEditorService;
        this.yamlSupport = yamlSupport;
        this.controlPlaneService = controlPlaneService;
        this.dynamicRoutingDataSource = dynamicRoutingDataSource;
        setSizeFull();
        setPadding(true);
        yamlArea.setWidthFull();
        yamlArea.setMinHeight("400px");
        Button applyYaml = new Button("Apply YAML", e -> applyYamlToModel());
        Button syncYaml = new Button("Sync from form", e -> syncYamlFromModel());
        yamlPanel.add(new Paragraph("YAML wins on Apply — confirm in dialog if form was edited."));
        yamlPanel.add(yamlMode, yamlArea, applyYaml, syncYaml);
        yamlPanel.setVisible(false);
        yamlMode.addValueChangeListener(e -> toggleYamlMode(e.getValue()));
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        removeAll();
        Set<String> jdbcNames = resolveJdbcNames();
        buildSteps(jdbcNames);
        if (parameter == null || "new".equalsIgnoreCase(parameter)) {
            model = TemplateEditorModel.fromPayload(templateEditorService.createEmptyDraft());
        } else {
            try {
                long id = Long.parseLong(parameter);
                model = TemplateEditorModel.fromPayload(templateEditorService.loadForEditor(id));
            } catch (NumberFormatException ex) {
                model = TemplateEditorModel.fromPayload(templateEditorService.createEmptyDraft());
                Notification.show("Invalid template id — started new draft");
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
                add(new RouterLink("Back to list", TemplateListView.class));
                return;
            }
        }
        renderEditor();
    }

    private void buildSteps(Set<String> jdbcNames) {
        steps.clear();
        ReviewStep reviewStep = new ReviewStep(controlPlaneService, this::saveTemplate);
        GeneralStep general = new GeneralStep();
        SourcesStep sources = new SourcesStep(jdbcNames);
        TransformStep transform = new TransformStep();
        SinksStep sinks = new SinksStep(jdbcNames);
        ExecutionStep execution = new ExecutionStep();
        reviewStep.setApplyAllSteps(this::applyAllStepsToModel);
        steps.put(new Tab("General"), general);
        steps.put(new Tab("Sources"), sources);
        steps.put(new Tab("Transform"), transform);
        steps.put(new Tab("Sinks"), sinks);
        steps.put(new Tab("Execution"), execution);
        steps.put(new Tab("Review"), reviewStep);
    }

    private void renderEditor() {
        H2 title = new H2(model.getTemplateId() == null ? "New template" : "Edit template " + model.getTemplateId());
        if (model.getKind() == TemplateDefinitionKind.V1) {
            add(new Paragraph("V1 legacy template — form is read-only. Use Migration tab (P4) or promote via REST."));
            if (model.getV1Yaml() != null) {
                TextArea v1 = new TextArea("V1 YAML (read-only)");
                v1.setValue(model.getV1Yaml());
                v1.setReadOnly(true);
                v1.setWidthFull();
                v1.setMinHeight("200px");
                add(v1);
            }
        }
        Tabs tabs = new Tabs(steps.keySet().toArray(Tab[]::new));
        stepContent.setPadding(false);
        stepContent.setSpacing(true);
        tabs.addSelectedChangeListener(e -> showStep(e.getSelectedTab()));
        add(title, yamlMode, tabs, stepContent, yamlPanel, new RouterLink("Back to templates", TemplateListView.class));
        refreshAllSteps();
        showStep(tabs.getSelectedTab() != null ? tabs.getSelectedTab() : tabs.getTabAt(0));
        syncYamlFromModel();
    }

    private void showStep(Tab tab) {
        stepContent.removeAll();
        EditorStep step = steps.get(tab);
        if (step != null) {
            stepContent.add(step.getView());
        }
    }

    private void refreshAllSteps() {
        steps.values().forEach(step -> step.refreshFromModel(model));
    }

    private void applyAllStepsToModel() {
        steps.values().forEach(step -> step.applyToModel(model));
    }

    private void toggleYamlMode(boolean advanced) {
        stepContent.setVisible(!advanced);
        yamlPanel.setVisible(advanced);
        if (advanced) {
            syncYamlFromModel();
        } else {
            applyYamlToModel();
            refreshAllSteps();
        }
    }

    private void syncYamlFromModel() {
        applyAllStepsToModel();
        yamlArea.setValue(yamlSupport.toYaml(model.getDraft()));
    }

    private void applyYamlToModel() {
        try {
            model.setDraft(yamlSupport.parseYaml(yamlArea.getValue()));
            refreshAllSteps();
            Notification.show("YAML applied");
        } catch (IllegalArgumentException ex) {
            Notification.show("YAML error: " + ex.getMessage());
        }
    }

    private void saveTemplate(TemplateEditorModel editorModel) {
        applyAllStepsToModel();
        try {
            TemplateEditorPayload saved;
            if (editorModel.getTemplateId() == null) {
                saved = templateEditorService.createAndSave(editorModel.getDraft());
                editorModel.setTemplateId(saved.templateId());
            } else {
                saved = templateEditorService.save(editorModel.getTemplateId(), editorModel.getDraft());
            }
            model = TemplateEditorModel.fromPayload(saved);
            Notification.show("Saved template " + saved.templateId());
            getUI().ifPresent(ui -> ui.navigate(TemplateListView.class));
        } catch (IllegalArgumentException ex) {
            Notification.show("Save failed: " + ex.getMessage());
        }
    }

    private Set<String> resolveJdbcNames() {
        if (dynamicRoutingDataSource == null || dynamicRoutingDataSource.getDataSources() == null) {
            return Collections.emptySet();
        }
        return dynamicRoutingDataSource.getDataSources().keySet();
    }
}
