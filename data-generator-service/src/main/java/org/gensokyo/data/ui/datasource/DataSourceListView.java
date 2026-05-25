/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.datasource;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.datasource.DataSourceConfigSummary;
import org.gensokyo.data.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * JDBC datasource administration (persisted {@code datasource_config} + runtime keys).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@Route(value = "datasources", layout = MainLayout.class)
@PageTitle("Datasources | Data Generator")
public class DataSourceListView extends VerticalLayout {

    private final DataSourceConfigService dataSourceConfigService;
    private final Grid<DataSourceConfigSummary> grid = new Grid<>(DataSourceConfigSummary.class, false);
    private final Span runtimeKeys = new Span();

    /**
     * @param dataSourceConfigService persistence and runtime registry
     */
    @Autowired
    public DataSourceListView(DataSourceConfigService dataSourceConfigService) {
        this.dataSourceConfigService = dataSourceConfigService;
        setSizeFull();
        setPadding(true);
        configureGrid();
        Button refresh = new Button("Refresh", e -> refresh());
        Button create = new Button("New datasource", e -> openCreateDialog());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout toolbar = new HorizontalLayout(create, refresh);
        toolbar.setAlignItems(Alignment.CENTER);
        add(toolbar, new H3("Persisted configs"), grid, new H3("Runtime keys (yaml + persisted)"), runtimeKeys);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(DataSourceConfigSummary::name).setHeader("Name").setSortable(true);
        grid.addColumn(DataSourceConfigSummary::url).setHeader("JDBC URL");
        grid.addColumn(DataSourceConfigSummary::driverClassName).setHeader("Driver");
        grid.addColumn(row -> row.enabled() ? "yes" : "no").setHeader("Enabled");
        grid.addComponentColumn(row -> {
            Button edit = new Button("Edit", e -> openEditDialog(row));
            Button test = new Button("Test", e -> test(row.name()));
            Button remove = new Button("Remove", e -> remove(row.name()));
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, test, remove);
        }).setHeader("Actions");
        grid.setSizeFull();
    }

    private void openCreateDialog() {
        DataSourceFormDialog dialog = new DataSourceFormDialog(dataSourceConfigService, this::refresh);
        dialog.open();
    }

    private void openEditDialog(DataSourceConfigSummary summary) {
        DataSourceFormDialog dialog = new DataSourceFormDialog(dataSourceConfigService, this::refresh);
        dialog.edit(summary);
        dialog.open();
    }

    private void test(String name) {
        try {
            Notification.show(dataSourceConfigService.testConnectionByName(name));
        } catch (Exception ex) {
            Notification.show("Test failed: " + ex.getMessage());
        }
    }

    private void remove(String name) {
        dataSourceConfigService.remove(name);
        Notification.show("Removed " + name);
        refresh();
    }

    private void refresh() {
        grid.setItems(dataSourceConfigService.listAll());
        runtimeKeys.setText(dataSourceConfigService.listRuntimeNames().stream()
                .sorted()
                .collect(Collectors.joining(", ")));
    }
}
