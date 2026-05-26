/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.migration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.template.migration.MigrationBacklogFilter;

import java.util.Arrays;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventorySummary;
import org.gensokyo.data.ui.ConsoleStyles;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.StatCard;
import org.gensokyo.data.ui.ViewPageHeader;
import org.gensokyo.data.ui.template.TemplateListView;
import org.gensokyo.data.ui.template.editor.TemplateEditorView;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Global migration inventory summary and backlog grid.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "migration", layout = MainLayout.class)
@PageTitle("Migration")
public class MigrationDashboardView extends VerticalLayout implements AfterNavigationObserver {

    private final MigrationConsoleService migrationConsoleService;
    private final Div summaryCards = new Div();
    private final Grid<MigrationInventoryEntry> grid = new Grid<>(MigrationInventoryEntry.class, false);
    private final ComboBox<String> filter = new ComboBox<>();

    /**
     * @param migrationConsoleService migration inventory facade
     */
    @Autowired
    public MigrationDashboardView(MigrationConsoleService migrationConsoleService) {
        this.migrationConsoleService = migrationConsoleService;
        ConsoleStyles.applyPage(this);
        setSizeFull();
        filter.setLabel(getTranslation("migration.filter"));
        filter.setItems(Arrays.stream(MigrationBacklogFilter.values()).map(Enum::name).toList());
        filter.setItemLabelGenerator(this::migrationFilterLabel);
        filter.setValue(MigrationBacklogFilter.ALL.name());
        filter.addValueChangeListener(e -> refreshBacklog());
        configureGrid();
        summaryCards.addClassName(ConsoleStyles.STAT_GRID);
        Button refresh = new Button(getTranslation("common.refresh"), e -> refreshAll());
        refresh.setIcon(VaadinIcon.REFRESH.create());
        Button templatesLink = new Button(getTranslation("migration.link.templates"), e ->
                e.getSource().getUI().ifPresent(ui -> ui.navigate(TemplateListView.class)));
        templatesLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        templatesLink.setIcon(VaadinIcon.FILE_TEXT.create());
        HorizontalLayout toolbar = new HorizontalLayout(filter, refresh, templatesLink);
        ConsoleStyles.applyToolbar(toolbar);
        ViewPageHeader header = new ViewPageHeader(
                getTranslation("migration.title"),
                getTranslation("migration.subtitle"));
        header.setToolbar(toolbar);
        VerticalLayout gridCard = new VerticalLayout(grid);
        ConsoleStyles.applyContentCard(gridCard);
        gridCard.setPadding(true);
        add(header, summaryCards, gridCard);
        refreshAll();
    }

    private void configureGrid() {
        grid.addColumn(MigrationInventoryEntry::getId).setHeader(getTranslation("migration.col.id")).setSortable(true);
        grid.addColumn(MigrationInventoryEntry::getName).setHeader(getTranslation("migration.col.name"));
        grid.addColumn(e -> e.getMigrationClass() != null ? e.getMigrationClass().name() : "")
                .setHeader(getTranslation("migration.col.class"));
        grid.addColumn(MigrationInventoryEntry::getScenarioFamily).setHeader(getTranslation("migration.col.family"));
        grid.addColumn(MigrationInventoryEntry::getWave).setHeader(getTranslation("migration.col.wave"));
        grid.addColumn(e -> e.isBusinessSignoffApproved()
                        ? getTranslation("common.yes")
                        : getTranslation("common.no"))
                .setHeader(getTranslation("migration.col.signoff"));
        grid.addColumn(MigrationInventoryEntry::getLastCompareReportPath).setHeader(getTranslation("migration.col.report"));
        grid.addComponentColumn(row -> {
            if (row.getDbTemplateId() == null) {
                return new com.vaadin.flow.component.html.Span("—");
            }
            Button open = new Button(getTranslation("migration.openEditor"), e -> getUI().ifPresent(
                    ui -> ui.navigate(TemplateEditorView.class, String.valueOf(row.getDbTemplateId()))));
            open.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            return open;
        }).setHeader(getTranslation("migration.col.actions"));
        grid.setSizeFull();
        ViewPageHeader.applyGridEmptyState(grid, getTranslation("grid.empty"));
    }

    private String migrationFilterLabel(String filterName) {
        return getTranslation("migration.filter." + filterName);
    }

    private void refreshAll() {
        try {
            MigrationInventorySummary summary = migrationConsoleService.summary();
            summaryCards.removeAll();
            summaryCards.add(
                    new StatCard(getTranslation("migration.stat.total"), summary.getTotalTemplates()),
                    new StatCard(getTranslation("migration.stat.db"), summary.getDatabaseTemplates()),
                    new StatCard(getTranslation("migration.stat.ready"), summary.getReadyToPromote()),
                    new StatCard(getTranslation("migration.stat.compat"), summary.getCompatibilityOnly()),
                    new StatCard(getTranslation("migration.stat.blocked"), summary.getBlocked()),
                    new StatCard(getTranslation("migration.stat.compare"), summary.getWithCompareReport()));
            refreshBacklog();
        } catch (Exception ex) {
            Notification.show(getTranslation("migration.summary.failed", ex.getMessage()));
        }
    }

    private void refreshBacklog() {
        try {
            grid.setItems(migrationConsoleService.backlog(filter.getValue()));
        } catch (Exception ex) {
            Notification.show(getTranslation("migration.backlog.failed", ex.getMessage()));
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getUI().ifPresent(ui -> ui.getPage().setTitle(getTranslation("page.migration")));
    }
}
