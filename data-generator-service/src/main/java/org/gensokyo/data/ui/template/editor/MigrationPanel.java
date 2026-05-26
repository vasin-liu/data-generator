/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.migration.MigrationBusinessSignoffRequest;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationCompareOptions;
import org.gensokyo.data.template.migration.MigrationComparisonReport;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.TemplateMigrationAnalysisDTO;
import org.gensokyo.data.ui.migration.MigrationConsoleService;

import java.util.function.Consumer;

/**
 * Per-template migration actions (analyze, draft, compare, sign-off, promote) for the editor.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class MigrationPanel extends VerticalLayout {

    private final MigrationConsoleService migrationConsoleService;
    private final TemplateEditorYamlSupport yamlSupport;
    private final Long templateId;
    private final TemplateDefinitionKind kind;
    private final Runnable onPromoted;
    private final Consumer<TemplateV2DraftVO> onDraftApply;

    private final Paragraph intro = new Paragraph();
    private final Paragraph status = new Paragraph();
    private final Paragraph inventoryStatus = new Paragraph();
    private final TextArea output = new TextArea();
    private final Button promoteButton = new Button();
    private final Button analyzeButton = new Button();
    private final Button draftButton = new Button();
    private final Button compareButton = new Button();
    private final Button signoffButton = new Button();

    private TemplateMigrationAnalysisDTO lastAnalysis;

    /**
     * @param migrationConsoleService migration facade
     * @param yamlSupport             YAML formatting for draft preview
     * @param templateId              persisted template id
     * @param kind                    detected definition kind
     * @param onPromoted              reload editor after promote
     * @param onDraftApply            optional apply draft to editor model
     */
    public MigrationPanel(
            MigrationConsoleService migrationConsoleService,
            TemplateEditorYamlSupport yamlSupport,
            Long templateId,
            TemplateDefinitionKind kind,
            Runnable onPromoted,
            Consumer<TemplateV2DraftVO> onDraftApply) {
        this.migrationConsoleService = migrationConsoleService;
        this.yamlSupport = yamlSupport;
        this.templateId = templateId;
        this.kind = kind;
        this.onPromoted = onPromoted;
        this.onDraftApply = onDraftApply;
        setPadding(false);
        output.setReadOnly(true);
        output.setWidthFull();
        output.setMinHeight("240px");
        applyI18n();
        configureButtons();
        Anchor docLink = new Anchor(
                "docs/migration/reports/builtin-orchestration-census.md",
                getTranslation("migration.panel.docLink"));
        docLink.setTarget("_blank");
        add(
                intro,
                docLink,
                status,
                inventoryStatus,
                new HorizontalLayout(analyzeButton, draftButton, compareButton, signoffButton, promoteButton),
                output);
        refreshInventoryHint();
        updatePromoteEnabled();
    }

    private void applyI18n() {
        intro.setText(getTranslation("migration.panel.intro", templateId));
        output.setLabel(getTranslation("migration.panel.result"));
        promoteButton.setText(getTranslation("migration.panel.promote"));
        analyzeButton.setText(getTranslation("migration.panel.analyze"));
        draftButton.setText(getTranslation("migration.panel.draft"));
        compareButton.setText(getTranslation("migration.panel.compare"));
        signoffButton.setText(getTranslation("migration.panel.signoff"));
    }

    private void configureButtons() {
        promoteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        promoteButton.addClickListener(e -> promote());
        analyzeButton.addClickListener(e -> analyze());
        draftButton.addClickListener(e -> showDraft());
        compareButton.addClickListener(e -> compare());
        signoffButton.addClickListener(e -> signoff());
    }

    private void analyze() {
        if (kind == TemplateDefinitionKind.V2) {
            Notification.show(getTranslation("migration.panel.alreadyV2"));
            return;
        }
        try {
            lastAnalysis = migrationConsoleService.analyze(templateId);
            status.setText(getTranslation(
                    "migration.panel.status",
                    lastAnalysis.getSuggestedClass(),
                    lastAnalysis.getRecommendedPath(),
                    lastAnalysis.getScenarioFamily(),
                    lastAnalysis.getWave()));
            output.setValue(formatAnalysis(lastAnalysis));
            updatePromoteEnabled();
        } catch (Exception ex) {
            Notification.show(getTranslation("migration.panel.analyze.failed", ex.getMessage()));
        }
    }

    private void showDraft() {
        try {
            TemplateV2DraftVO draft = migrationConsoleService.buildDraft(templateId);
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle(getTranslation("migration.panel.draft.title"));
            TextArea yaml = new TextArea();
            yaml.setValue(yamlSupport.toYaml(draft));
            yaml.setReadOnly(true);
            yaml.setWidthFull();
            yaml.setMinHeight("360px");
            Button close = new Button(getTranslation("common.close"), e -> dialog.close());
            Button apply = new Button(getTranslation("migration.panel.draft.apply"), e -> {
                if (onDraftApply != null) {
                    onDraftApply.accept(draft);
                    Notification.show(getTranslation("migration.panel.draft.applied"));
                }
                dialog.close();
            });
            apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            dialog.add(yaml);
            dialog.getFooter().add(close, apply);
            dialog.setWidth("48rem");
            dialog.open();
        } catch (Exception ex) {
            Notification.show(getTranslation("migration.panel.draft.failed", ex.getMessage()));
        }
    }

    private void compare() {
        try {
            MigrationComparisonReport report =
                    migrationConsoleService.compare(templateId, new MigrationCompareOptions());
            output.setValue(String.format(
                    "Compare: class=%s recommendation=%s v1Rows=%d v2Rows=%d match=%.2f%% report=%s%n%s",
                    report.getClassification(),
                    report.getRecommendation(),
                    report.getV1RowCount(),
                    report.getV2RowCount(),
                    report.getSampleMatchRate() * 100.0,
                    report.getReportPath(),
                    report.getWarnings()));
            refreshInventoryHint();
            updatePromoteEnabled();
        } catch (Exception ex) {
            Notification.show(getTranslation("migration.panel.compare.failed", ex.getMessage()));
        }
    }

    private void signoff() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("migration.panel.signoff.title", templateId));
        TextField approvedBy = new TextField();
        approvedBy.setLabel(getTranslation("migration.panel.signoff.approvedBy"));
        TextArea notes = new TextArea();
        notes.setLabel(getTranslation("migration.panel.signoff.notes"));
        Button save = new Button(getTranslation("migration.panel.signoff.record"), e -> {
            try {
                MigrationBusinessSignoffRequest request = new MigrationBusinessSignoffRequest();
                request.setApproved(true);
                request.setApprovedBy(approvedBy.getValue());
                request.setNotes(notes.getValue());
                MigrationInventoryEntry entry = migrationConsoleService.signoff(templateId, request);
                output.setValue(getTranslation(
                        "migration.panel.signoff.done",
                        entry.getId(),
                        entry.getBusinessSignoffAt()));
                dialog.close();
                refreshInventoryHint();
            } catch (Exception ex) {
                Notification.show(getTranslation("migration.panel.signoff.failed", ex.getMessage()));
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(approvedBy, notes);
        dialog.getFooter().add(new Button(getTranslation("common.cancel"), e -> dialog.close()), save);
        dialog.open();
    }

    private void promote() {
        try {
            migrationConsoleService.promote(templateId);
            Notification.show(getTranslation("migration.panel.promote.done"));
            onPromoted.run();
        } catch (Exception ex) {
            Notification.show(getTranslation("migration.panel.promote.failed", ex.getMessage()));
        }
    }

    private void refreshInventoryHint() {
        migrationConsoleService.inventoryForTemplate(templateId).ifPresentOrElse(
                entry -> inventoryStatus.setText(getTranslation(
                        "migration.panel.inventory",
                        entry.getMigrationClass(),
                        entry.isBusinessSignoffApproved()
                                ? getTranslation("migration.panel.inventory.signed")
                                : getTranslation("migration.panel.inventory.pending"))),
                () -> inventoryStatus.setText(getTranslation("migration.panel.inventory.missing", templateId)));
    }

    private void updatePromoteEnabled() {
        boolean blocked = isPromoteBlocked();
        promoteButton.setEnabled(!blocked);
        if (blocked && lastAnalysis != null
                && lastAnalysis.getSuggestedClass() == MigrationClassification.COMPATIBILITY_ONLY) {
            promoteButton.setTooltipText(getTranslation("migration.panel.promote.blocked"));
        }
    }

    private boolean isPromoteBlocked() {
        if (kind == TemplateDefinitionKind.V2) {
            return true;
        }
        if (lastAnalysis != null) {
            if (lastAnalysis.getSuggestedClass() == MigrationClassification.COMPATIBILITY_ONLY
                    || "compatibility_only".equalsIgnoreCase(lastAnalysis.getRecommendedPath())) {
                return true;
            }
        }
        return migrationConsoleService.inventoryForTemplate(templateId)
                .map(entry -> entry.getMigrationClass() == MigrationClassification.COMPATIBILITY_ONLY
                        || entry.getMigrationClass() == MigrationClassification.BLOCKED)
                .orElse(false);
    }

    private static String formatAnalysis(TemplateMigrationAnalysisDTO analysis) {
        StringBuilder sb = new StringBuilder();
        if (analysis.getBlockers() != null && !analysis.getBlockers().isEmpty()) {
            sb.append("Blockers:\n").append(String.join("\n", analysis.getBlockers())).append("\n\n");
        }
        if (analysis.getWarnings() != null && !analysis.getWarnings().isEmpty()) {
            sb.append("Warnings:\n").append(String.join("\n", analysis.getWarnings()));
        }
        return sb.toString();
    }
}
