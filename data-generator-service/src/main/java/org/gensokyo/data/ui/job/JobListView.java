/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.job;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionStatus;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.gensokyo.data.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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

    private static final int POLL_MS = 2000;

    private final TaskExecutionService taskExecutionService;
    private final Grid<TaskExecutionSummary> grid = new Grid<>(TaskExecutionSummary.class, false);
    private final TextField templateIdFilter = new TextField("Template id");
    private final Span pollHint = new Span();
    private Registration pollRegistration;

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
        pollHint.addClassNames("text-secondary");
        HorizontalLayout toolbar = new HorizontalLayout(templateIdFilter, refresh, pollHint);
        add(toolbar, grid);
        refreshGrid();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        if (ui != null) {
            pollRegistration = ui.addPollListener(event -> refreshGrid());
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        UI ui = detachEvent.getUI();
        if (ui != null) {
            ui.setPollInterval(-1);
        }
        super.onDetach(detachEvent);
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
        try {
            Long templateId = parseTemplateIdFilter();
            List<TaskExecutionSummary> items = taskExecutionService.list(templateId);
            grid.setItems(items);
            updatePolling(items);
        } catch (NumberFormatException ex) {
            Notification.show("Invalid template id filter");
        }
    }

    private Long parseTemplateIdFilter() {
        String raw = templateIdFilter.getValue();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Long.parseLong(raw.trim());
    }

    private void updatePolling(List<TaskExecutionSummary> items) {
        boolean active = items.stream().anyMatch(JobListView::isActive);
        getUI().ifPresent(ui -> {
            if (active) {
                ui.setPollInterval(POLL_MS);
                pollHint.setText("Auto-refresh every 2s (active runs)");
            } else {
                ui.setPollInterval(-1);
                pollHint.setText("");
            }
        });
    }

    private static boolean isActive(TaskExecutionSummary row) {
        String status = row.status();
        return TaskExecutionStatus.QUEUED.name().equals(status)
                || TaskExecutionStatus.RUNNING.name().equals(status);
    }
}
