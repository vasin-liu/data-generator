/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.TemplateV2ControlPlaneService;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.gensokyo.data.ui.ConsoleStyles;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.template.TemplateListView;
import org.gensokyo.data.ui.template.editor.steps.ExecutionStep;
import org.gensokyo.data.ui.template.editor.steps.GeneralStep;
import org.gensokyo.data.ui.template.editor.steps.ReviewStep;
import org.gensokyo.data.ui.template.editor.steps.SinksStep;
import org.gensokyo.data.ui.template.editor.steps.SourcesStep;
import org.gensokyo.data.ui.template.editor.steps.TransformStep;
import org.gensokyo.data.ui.migration.MigrationConsoleService;
import org.springframework.beans.factory.annotation.Autowired;

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
@Route(value = "template/editor/:templateId?", layout = MainLayout.class)
@PageTitle("Template editor | Data Generator")
public class TemplateEditorView extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private final TemplateEditorService templateEditorService;
    private final TemplateEditorYamlSupport yamlSupport;
    private final TemplateV2ControlPlaneService controlPlaneService;
    private final DynamicRoutingDataSource dynamicRoutingDataSource;
    private final MigrationConsoleService migrationConsoleService;
    private final TemplateEditorRunSupport templateEditorRunSupport;

    private final VerticalLayout stepContent = new VerticalLayout();
    private Tab migrationTab;
    private MigrationPanel migrationPanel;
    private final Div yamlPanel = new Div();
    private final TextArea yamlArea = new TextArea();
    private final Checkbox yamlMode = new Checkbox();
    private final Map<Tab, EditorStep> steps = new LinkedHashMap<>();
    private TemplateEditorModel model;

    /**
     * Spring-injected editor view.
     *
     * @param templateEditorService   persistence
     * @param yamlSupport             YAML parse/dump
     * @param controlPlaneService     validate
     * @param dynamicRoutingDataSource optional JDBC name list
     * @param migrationConsoleService  V1→V2 migration actions
     * @param templateEditorRunSupport save + run from Review step
     */
    @Autowired
    public TemplateEditorView(
            TemplateEditorService templateEditorService,
            TemplateEditorYamlSupport yamlSupport,
            TemplateV2ControlPlaneService controlPlaneService,
            MigrationConsoleService migrationConsoleService,
            TemplateEditorRunSupport templateEditorRunSupport,
            @Autowired(required = false) DynamicRoutingDataSource dynamicRoutingDataSource) {
        this.templateEditorService = templateEditorService;
        this.yamlSupport = yamlSupport;
        this.controlPlaneService = controlPlaneService;
        this.migrationConsoleService = migrationConsoleService;
        this.templateEditorRunSupport = templateEditorRunSupport;
        this.dynamicRoutingDataSource = dynamicRoutingDataSource;
        ConsoleStyles.applyPage(this);
        setSizeFull();
        yamlArea.setWidthFull();
        yamlArea.setMinHeight("400px");
        yamlArea.setLabel(getTranslation("editor.yaml.content"));
        yamlMode.setLabel(getTranslation("editor.yaml.mode"));
        Button applyYaml = new Button(getTranslation("editor.yaml.apply"), e -> confirmApplyYaml());
        applyYaml.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button syncYaml = new Button(getTranslation("editor.yaml.sync"), e -> syncYamlFromModel());
        syncYaml.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        yamlPanel.addClassName(ConsoleStyles.YAML_PANEL);
        yamlPanel.add(new Paragraph(getTranslation("editor.yaml.hint")));
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
                Notification.show(getTranslation("editor.invalidId"));
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
                add(backToListButton());
                return;
            }
        }
        renderEditor();
    }

    private void buildSteps(Set<String> jdbcNames) {
        steps.clear();
        ReviewStep reviewStep = new ReviewStep(
                controlPlaneService,
                templateEditorRunSupport,
                this::saveTemplate,
                this::assignTemplateIdAfterRun);
        GeneralStep general = new GeneralStep();
        SourcesStep sources = new SourcesStep(jdbcNames);
        TransformStep transform = new TransformStep();
        SinksStep sinks = new SinksStep(jdbcNames);
        ExecutionStep execution = new ExecutionStep();
        reviewStep.setApplyAllSteps(this::applyAllStepsToModel);
        migrationTab = new Tab(getTranslation("editor.tab.migration"));
        steps.put(new Tab(getTranslation("editor.tab.general")), general);
        steps.put(new Tab(getTranslation("editor.tab.sources")), sources);
        steps.put(new Tab(getTranslation("editor.tab.transform")), transform);
        steps.put(new Tab(getTranslation("editor.tab.sinks")), sinks);
        steps.put(new Tab(getTranslation("editor.tab.execution")), execution);
        steps.put(new Tab(getTranslation("editor.tab.review")), reviewStep);
    }

    private void renderEditor() {
        String titleText = model.getTemplateId() == null
                ? getTranslation("editor.new")
                : getTranslation("editor.edit", model.getTemplateId());
        H2 title = new H2(titleText);
        title.addClassNames(ConsoleStyles.PAGE_TITLE, ConsoleStyles.EDITOR_TITLE);
        if (model.getKind() == TemplateDefinitionKind.V1) {
            Paragraph v1Note = new Paragraph(getTranslation("editor.v1.note"));
            v1Note.addClassName(ConsoleStyles.PAGE_SUBTITLE);
            add(v1Note);
            if (model.getV1Yaml() != null) {
                TextArea v1 = new TextArea(getTranslation("editor.v1.yaml"));
                v1.setValue(model.getV1Yaml());
                v1.setReadOnly(true);
                v1.setWidthFull();
                v1.setMinHeight("200px");
                add(v1);
            }
        }
        java.util.List<Tab> tabList = new java.util.ArrayList<>(steps.keySet());
        if (model.getTemplateId() != null) {
            tabList.add(migrationTab);
        }
        Tabs tabs = new Tabs(tabList.toArray(Tab[]::new));
        stepContent.addClassName(ConsoleStyles.EDITOR_STEP);
        stepContent.setPadding(false);
        stepContent.setSpacing(true);
        tabs.addSelectedChangeListener(e -> showSelectedTab(e.getSelectedTab()));
        VerticalLayout editorCard = new VerticalLayout(yamlMode, tabs, stepContent, yamlPanel);
        ConsoleStyles.applyContentCard(editorCard);
        editorCard.addClassName(ConsoleStyles.EDITOR_CARD);
        Button back = backToListButton();
        add(title, editorCard, back);
        setFlexGrow(1, editorCard);
        if (model.getTemplateId() != null) {
            migrationPanel = new MigrationPanel(
                    migrationConsoleService,
                    yamlSupport,
                    model.getTemplateId(),
                    model.getKind(),
                    this::reloadAfterPromote,
                    draft -> {
                        model.setDraft(draft);
                        refreshAllSteps();
                        syncYamlFromModel();
                    });
        }
        refreshAllSteps();
        showSelectedTab(tabs.getSelectedTab() != null ? tabs.getSelectedTab() : tabs.getTabAt(0));
        syncYamlFromModel();
    }

    private void showSelectedTab(Tab tab) {
        if (tab == migrationTab) {
            stepContent.setVisible(true);
            yamlPanel.setVisible(false);
            stepContent.removeAll();
            if (migrationPanel != null) {
                stepContent.add(migrationPanel);
            } else {
                stepContent.add(new Paragraph(getTranslation("editor.migration.saveFirst")));
            }
            return;
        }
        yamlMode.setValue(false);
        showStep(tab);
    }

    private void showStep(Tab tab) {
        stepContent.setVisible(true);
        yamlPanel.setVisible(false);
        stepContent.removeAll();
        EditorStep step = steps.get(tab);
        if (step != null) {
            stepContent.add(step.getView());
        }
    }

    private void assignTemplateIdAfterRun(Long templateId) {
        model.setTemplateId(templateId);
    }

    private void reloadEditorUi() {
        removeAll();
        buildSteps(resolveJdbcNames());
        renderEditor();
    }

    private void reloadAfterPromote() {
        if (model.getTemplateId() == null) {
            return;
        }
        try {
            model = TemplateEditorModel.fromPayload(templateEditorService.loadForEditor(model.getTemplateId()));
            reloadEditorUi();
            Notification.show(getTranslation("review.reload.promoted"));
        } catch (IllegalArgumentException ex) {
            Notification.show(getTranslation("review.reload.failed", ex.getMessage()));
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

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getUI().ifPresent(ui -> ui.getPage().setTitle(getTranslation("page.editor")));
    }

    private void confirmApplyYaml() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("editor.yaml.confirm.title"));
        dialog.setText(getTranslation("editor.yaml.confirm.text"));
        dialog.setConfirmText(getTranslation("editor.yaml.apply"));
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmButtonTheme("primary");
        dialog.addConfirmListener(e -> applyYamlToModel());
        dialog.open();
    }

    private void applyYamlToModel() {
        try {
            model.setDraft(yamlSupport.parseYaml(yamlArea.getValue()));
            refreshAllSteps();
            Notification.show(getTranslation("review.yaml.applied"));
        } catch (IllegalArgumentException ex) {
            Notification.show(getTranslation("review.yaml.error", ex.getMessage()));
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
            Notification.show(getTranslation("review.saved", saved.templateId()));
            reloadEditorUi();
        } catch (IllegalArgumentException ex) {
            Notification.show(getTranslation("review.save.failed", ex.getMessage()));
        }
    }

    private Button backToListButton() {
        Button back = new Button(getTranslation("editor.back"), e ->
                e.getSource().getUI().ifPresent(ui -> ui.navigate(TemplateListView.class)));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.addClassName(ConsoleStyles.EDITOR_BACK);
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        return back;
    }

    private Set<String> resolveJdbcNames() {
        if (dynamicRoutingDataSource == null || dynamicRoutingDataSource.getDataSources() == null) {
            return Collections.emptySet();
        }
        return dynamicRoutingDataSource.getDataSources().keySet();
    }
}
