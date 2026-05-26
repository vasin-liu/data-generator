/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.datasource;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.datasource.DataSourceConfigSummary;
import org.gensokyo.data.ui.ConsoleStyles;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.ViewPageHeader;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Collectors;

/**
 * JDBC datasource administration (persisted {@code datasource_config} + runtime keys).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "datasources", layout = MainLayout.class)
@PageTitle("Datasources")
public class DataSourceListView extends VerticalLayout implements AfterNavigationObserver {

    private final DataSourceConfigService dataSourceConfigService;
    private final Grid<DataSourceConfigSummary> grid = new Grid<>(DataSourceConfigSummary.class, false);
    private final Span runtimeKeys = new Span();

    /**
     * @param dataSourceConfigService persistence and runtime registry
     */
    @Autowired
    public DataSourceListView(DataSourceConfigService dataSourceConfigService) {
        this.dataSourceConfigService = dataSourceConfigService;
        ConsoleStyles.applyPage(this);
        setSizeFull();
        configureGrid();
        Button refresh = new Button(getTranslation("common.refresh"), e -> refresh());
        refresh.setIcon(VaadinIcon.REFRESH.create());
        Button create = new Button(getTranslation("datasources.new"), e -> openCreateDialog());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.setIcon(VaadinIcon.PLUS.create());
        HorizontalLayout toolbar = new HorizontalLayout(create, refresh);
        toolbar.setAlignItems(Alignment.CENTER);
        ConsoleStyles.applyToolbar(toolbar);
        ViewPageHeader header = new ViewPageHeader(
                getTranslation("datasources.title"),
                getTranslation("datasources.subtitle"));
        header.setToolbar(toolbar);
        H3 persistedTitle = new H3(getTranslation("datasources.section.persisted"));
        persistedTitle.addClassName(ConsoleStyles.SECTION_TITLE);
        VerticalLayout gridCard = new VerticalLayout(grid);
        ConsoleStyles.applyContentCard(gridCard);
        H3 runtimeTitle = new H3(getTranslation("datasources.section.runtime"));
        runtimeTitle.addClassName(ConsoleStyles.SECTION_TITLE);
        runtimeKeys.addClassName(ConsoleStyles.PAGE_SUBTITLE);
        add(header, persistedTitle, gridCard, runtimeTitle, runtimeKeys);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(DataSourceConfigSummary::name).setHeader(getTranslation("datasources.col.name")).setSortable(true);
        grid.addColumn(DataSourceConfigSummary::url).setHeader(getTranslation("datasources.col.url"));
        grid.addColumn(DataSourceConfigSummary::driverClassName).setHeader(getTranslation("datasources.col.driver"));
        grid.addColumn(row -> row.enabled() ? getTranslation("common.yes") : getTranslation("common.no"))
                .setHeader(getTranslation("datasources.col.enabled"));
        grid.addComponentColumn(row -> {
            Button edit = new Button(getTranslation("common.edit"), e -> openEditDialog(row));
            Button test = new Button(getTranslation("common.test"), e -> test(row.name()));
            Button remove = new Button(getTranslation("common.remove"), e -> confirmRemove(row.name()));
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, test, remove);
        }).setHeader(getTranslation("datasources.col.actions"));
        grid.setSizeFull();
        ViewPageHeader.applyGridEmptyState(grid, getTranslation("grid.empty"));
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
            Notification.show(getTranslation("datasources.dialog.test.failed", ex.getMessage()));
        }
    }

    private void confirmRemove(String name) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("datasources.remove.confirm.title", name));
        dialog.setText(getTranslation("datasources.remove.confirm.text"));
        dialog.setConfirmText(getTranslation("common.remove"));
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> remove(name));
        dialog.open();
    }

    private void remove(String name) {
        dataSourceConfigService.remove(name);
        Notification.show(getTranslation("datasources.removed", name));
        refresh();
    }

    private void refresh() {
        grid.setItems(dataSourceConfigService.listAll());
        runtimeKeys.setText(dataSourceConfigService.listRuntimeNames().stream()
                .sorted()
                .collect(Collectors.joining(", ")));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getUI().ifPresent(ui -> ui.getPage().setTitle(getTranslation("page.datasources")));
    }
}
