/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.migration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.gensokyo.data.template.migration.MigrationBacklogFilter;
import org.gensokyo.data.template.migration.MigrationInventoryEntry;
import org.gensokyo.data.template.migration.MigrationInventorySummary;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.template.TemplateListView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Global migration inventory summary and backlog grid.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@Route(value = "migration", layout = MainLayout.class)
@PageTitle("Migration | Data Generator")
public class MigrationDashboardView extends VerticalLayout {

    private final MigrationConsoleService migrationConsoleService;
    private final Div summaryCards = new Div();
    private final Grid<MigrationInventoryEntry> grid = new Grid<>(MigrationInventoryEntry.class, false);
    private final ComboBox<String> filter = new ComboBox<>("Backlog filter");

    /**
     * @param migrationConsoleService migration inventory facade
     */
    @Autowired
    public MigrationDashboardView(MigrationConsoleService migrationConsoleService) {
        this.migrationConsoleService = migrationConsoleService;
        setSizeFull();
        setPadding(true);
        filter.setItems(
                MigrationBacklogFilter.ALL.name(),
                MigrationBacklogFilter.READY.name(),
                MigrationBacklogFilter.BLOCKED.name(),
                MigrationBacklogFilter.COMPATIBILITY_ONLY.name(),
                MigrationBacklogFilter.NEEDS_COMPARE.name(),
                MigrationBacklogFilter.PENDING_SIGNOFF.name());
        filter.setValue(MigrationBacklogFilter.ALL.name());
        filter.addValueChangeListener(e -> refreshBacklog());
        configureGrid();
        Button refresh = new Button("Refresh", e -> refreshAll());
        add(
                new H2("Migration dashboard"),
                new Span("Promote is disabled for COMPATIBILITY_ONLY templates (W3 policy S1). "
                        + "See docs/migration/reports/builtin-orchestration-census.md"),
                summaryCards,
                new HorizontalLayout(filter, refresh),
                grid,
                new RouterLink("Templates", TemplateListView.class));
        refreshAll();
    }

    private void configureGrid() {
        grid.addColumn(MigrationInventoryEntry::getId).setHeader("Id").setSortable(true);
        grid.addColumn(MigrationInventoryEntry::getName).setHeader("Name");
        grid.addColumn(e -> e.getMigrationClass() != null ? e.getMigrationClass().name() : "")
                .setHeader("Class");
        grid.addColumn(MigrationInventoryEntry::getScenarioFamily).setHeader("Family");
        grid.addColumn(MigrationInventoryEntry::getWave).setHeader("Wave");
        grid.addColumn(e -> e.isBusinessSignoffApproved() ? "yes" : "no").setHeader("Sign-off");
        grid.addColumn(MigrationInventoryEntry::getLastCompareReportPath).setHeader("Report");
        grid.setSizeFull();
    }

    private void refreshAll() {
        try {
            MigrationInventorySummary summary = migrationConsoleService.summary();
            summaryCards.removeAll();
            summaryCards.add(
                    card("Total", summary.getTotalTemplates()),
                    card("DB templates", summary.getDatabaseTemplates()),
                    card("Ready to promote", summary.getReadyToPromote()),
                    card("Compatibility only", summary.getCompatibilityOnly()),
                    card("Blocked", summary.getBlocked()),
                    card("With compare", summary.getWithCompareReport()));
            refreshBacklog();
        } catch (Exception ex) {
            Notification.show("Failed to load summary: " + ex.getMessage());
        }
    }

    private void refreshBacklog() {
        try {
            grid.setItems(migrationConsoleService.backlog(filter.getValue()));
        } catch (Exception ex) {
            Notification.show("Failed to load backlog: " + ex.getMessage());
        }
    }

    private static Div card(String label, int value) {
        Div div = new Div(new Span(label + ": " + value));
        div.getStyle().set("padding", "var(--lumo-space-s)");
        div.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        div.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        div.getStyle().set("margin-right", "var(--lumo-space-s)");
        return div;
    }
}
