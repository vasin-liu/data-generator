/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.gensokyo.data.ui.ConsoleStyles;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.ViewPageHeader;
import org.gensokyo.data.ui.job.JobDetailView;
import org.gensokyo.data.ui.template.editor.TemplateEditorRunSupport;
import org.gensokyo.data.ui.template.editor.TemplateEditorView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;

/**
 * Template catalog with navigation to the V2 editor.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "templates", layout = MainLayout.class)
@PageTitle("Templates")
public class TemplateListView extends VerticalLayout implements AfterNavigationObserver {

    private final TemplateRepository templateRepository;
    private final TemplateEditorService templateEditorService;
    private final TemplateEditorRunSupport templateEditorRunSupport;
    private final Grid<TemplatePO> grid = new Grid<>(TemplatePO.class, false);
    private final Checkbox includeArchived = new Checkbox();
    private final TextField nameFilter = new TextField();

    /**
     * @param templateRepository  template persistence
     * @param templateEditorService archive/restore
     * @param templateEditorRunSupport start runs from the grid
     */
    @Autowired
    public TemplateListView(
            TemplateRepository templateRepository,
            TemplateEditorService templateEditorService,
            TemplateEditorRunSupport templateEditorRunSupport) {
        this.templateRepository = templateRepository;
        this.templateEditorService = templateEditorService;
        this.templateEditorRunSupport = templateEditorRunSupport;
        ConsoleStyles.applyPage(this);
        setSizeFull();
        applyI18n();
        configureGrid();
        Button refresh = new Button(getTranslation("common.refresh"), e -> refreshGrid());
        refresh.setIcon(VaadinIcon.REFRESH.create());
        Button create = new Button(getTranslation("templates.new"), e -> getUI().ifPresent(
                ui -> ui.navigate(TemplateEditorView.class, "new")));
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.setIcon(VaadinIcon.PLUS.create());
        nameFilter.setClearButtonVisible(true);
        nameFilter.setWidth("14rem");
        nameFilter.setPrefixComponent(VaadinIcon.SEARCH.create());
        nameFilter.addValueChangeListener(e -> refreshGrid());
        includeArchived.addValueChangeListener(e -> refreshGrid());
        HorizontalLayout toolbar = new HorizontalLayout(create, refresh, nameFilter, includeArchived);
        toolbar.setAlignItems(Alignment.CENTER);
        ConsoleStyles.applyToolbar(toolbar);
        ViewPageHeader header = new ViewPageHeader(
                getTranslation("templates.title"),
                getTranslation("templates.subtitle"));
        header.setToolbar(toolbar);
        VerticalLayout gridCard = new VerticalLayout(grid);
        ConsoleStyles.applyContentCard(gridCard);
        add(header, gridCard);
        refreshGrid();
    }

    private void applyI18n() {
        includeArchived.setLabel(getTranslation("templates.includeArchived"));
        nameFilter.setLabel(getTranslation("templates.filter.name"));
        nameFilter.setPlaceholder(getTranslation("templates.filter.placeholder"));
    }

    private void configureGrid() {
        grid.addColumn(TemplatePO::getId).setHeader(getTranslation("templates.col.id")).setSortable(true);
        grid.addColumn(TemplatePO::getName).setHeader(getTranslation("templates.col.name")).setSortable(true);
        grid.addColumn(row -> Boolean.TRUE.equals(row.getArchived())
                        ? getTranslation("common.yes")
                        : getTranslation("common.no"))
                .setHeader(getTranslation("templates.col.archived"));
        grid.addComponentColumn(row -> {
            Button edit = new Button(getTranslation("common.edit"), e -> getUI().ifPresent(
                    ui -> ui.navigate(TemplateEditorView.class, String.valueOf(row.getId()))));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            Button run = new Button(getTranslation("common.run"));
            run.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            boolean active = !Boolean.TRUE.equals(row.getArchived());
            run.setEnabled(active);
            run.addClickListener(e -> confirmRunTemplate(row));
            String archiveLabel = Boolean.TRUE.equals(row.getArchived())
                    ? getTranslation("common.restore")
                    : getTranslation("common.archive");
            Button archive = new Button(archiveLabel);
            archive.addClickListener(e -> confirmToggleArchive(row));
            HorizontalLayout actions = new HorizontalLayout(edit, run, archive);
            actions.setAlignItems(Alignment.CENTER);
            return actions;
        }).setHeader(getTranslation("templates.col.actions"));
        grid.setSizeFull();
        ViewPageHeader.applyGridEmptyState(grid, getTranslation("grid.empty"));
    }

    private void refreshGrid() {
        List<TemplatePO> rows = includeArchived.getValue()
                ? templateRepository.findAll()
                : templateRepository.findByArchivedFalse();
        String needle = nameFilter.getValue();
        if (needle != null && !needle.isBlank()) {
            String lower = needle.toLowerCase(Locale.ROOT);
            rows = rows.stream()
                    .filter(t -> matchesFilter(t, lower))
                    .toList();
        }
        grid.setItems(rows);
    }

    private static boolean matchesFilter(TemplatePO row, String lower) {
        if (row.getName() != null && row.getName().toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        return row.getId() != null && String.valueOf(row.getId()).contains(lower);
    }

    private void confirmRunTemplate(TemplatePO row) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("templates.run.confirm.title", row.getName()));
        dialog.setText(getTranslation("templates.run.confirm.text"));
        dialog.setConfirmText(getTranslation("common.run"));
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmButtonTheme("primary");
        dialog.addConfirmListener(e -> runTemplate(row));
        dialog.open();
    }

    private void runTemplate(TemplatePO row) {
        try {
            TemplateEditorRunSupport.RunStartResult started = templateEditorRunSupport.runExisting(row.getId());
            Notification.show(getTranslation("templates.run.started"));
            getUI().ifPresent(ui -> ui.navigate(JobDetailView.class, String.valueOf(started.instanceId())));
        } catch (IllegalArgumentException ex) {
            Notification.show(getTranslation("templates.run.failed", ex.getMessage()));
        }
    }

    private void confirmToggleArchive(TemplatePO row) {
        if (Boolean.TRUE.equals(row.getArchived())) {
            doToggleArchive(row);
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("templates.archive.confirm.title", row.getName()));
        dialog.setText(getTranslation("templates.archive.confirm.text"));
        dialog.setConfirmText(getTranslation("common.archive"));
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> doToggleArchive(row));
        dialog.open();
    }

    private void doToggleArchive(TemplatePO row) {
        try {
            if (Boolean.TRUE.equals(row.getArchived())) {
                templateEditorService.restore(row.getId());
                Notification.show(getTranslation("templates.restored", row.getId()));
            } else {
                templateEditorService.archive(row.getId());
                Notification.show(getTranslation("templates.archived", row.getId()));
            }
            refreshGrid();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getUI().ifPresent(ui -> ui.getPage().setTitle(getTranslation("page.templates")));
    }
}
