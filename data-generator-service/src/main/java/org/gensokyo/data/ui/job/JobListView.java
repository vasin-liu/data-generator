/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.job;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.gensokyo.data.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Task execution history grid for the operator job center.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@Route(value = "jobs", layout = MainLayout.class)
@PageTitle("Jobs | Data Generator")
public class JobListView extends VerticalLayout {

    private final TaskExecutionService taskExecutionService;
    private final Grid<TaskExecutionSummary> grid = new Grid<>(TaskExecutionSummary.class, false);
    private final TextField templateIdFilter = new TextField("Template id");

    /**
     * @param taskExecutionService execution history
     */
    @Autowired
    public JobListView(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
        setSizeFull();
        setPadding(true);
        configureGrid();
        Button refresh = new Button("Refresh", e -> refreshGrid());
        templateIdFilter.setPlaceholder("optional");
        templateIdFilter.setWidth("12rem");
        templateIdFilter.addValueChangeListener(e -> refreshGrid());
        HorizontalLayout toolbar = new HorizontalLayout(templateIdFilter, refresh);
        add(toolbar, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(TaskExecutionSummary::instanceId).setHeader("Instance").setSortable(true);
        grid.addColumn(TaskExecutionSummary::templateName).setHeader("Template");
        grid.addColumn(TaskExecutionSummary::definitionKind).setHeader("Kind");
        grid.addColumn(TaskExecutionSummary::status).setHeader("Status");
        grid.addColumn(TaskExecutionSummary::finishedAt).setHeader("Finished");
        grid.addComponentColumn(row -> new RouterLink("Detail", JobDetailView.class, String.valueOf(row.instanceId())))
                .setHeader("Detail");
        grid.setSizeFull();
    }

    private void refreshGrid() {
        Long templateId = null;
        String raw = templateIdFilter.getValue();
        if (raw != null && !raw.isBlank()) {
            templateId = Long.parseLong(raw.trim());
        }
        grid.setItems(taskExecutionService.list(templateId));
    }
}
