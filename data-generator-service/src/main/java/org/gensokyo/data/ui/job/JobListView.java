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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionStatus;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.gensokyo.data.ui.ConsoleStyles;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.ViewPageHeader;
import org.gensokyo.data.ui.template.editor.TemplateEditorView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Task execution history grid for the operator job center.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "jobs", layout = MainLayout.class)
@PageTitle("Jobs")
public class JobListView extends VerticalLayout implements AfterNavigationObserver {

    private static final int POLL_MS = 2000;

    private final TaskExecutionService taskExecutionService;
    private final Grid<TaskExecutionSummary> grid = new Grid<>(TaskExecutionSummary.class, false);
    private final TextField templateIdFilter = new TextField();
    private final Span pollHint = new Span();
    private Registration pollRegistration;

    /**
     * @param taskExecutionService execution history
     */
    @Autowired
    public JobListView(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
        ConsoleStyles.applyPage(this);
        setSizeFull();
        templateIdFilter.setLabel(getTranslation("jobs.filter.templateId"));
        templateIdFilter.setPlaceholder(getTranslation("jobs.filter.placeholder"));
        configureGrid();
        Button refresh = new Button(getTranslation("common.refresh"), e -> refreshGrid());
        refresh.setIcon(VaadinIcon.REFRESH.create());
        templateIdFilter.setWidth("12rem");
        templateIdFilter.setPrefixComponent(VaadinIcon.SEARCH.create());
        templateIdFilter.addValueChangeListener(e -> refreshGrid());
        pollHint.addClassName(ConsoleStyles.PAGE_SUBTITLE);
        HorizontalLayout toolbar = new HorizontalLayout(templateIdFilter, refresh, pollHint);
        ConsoleStyles.applyToolbar(toolbar);
        ViewPageHeader header = new ViewPageHeader(
                getTranslation("jobs.title"),
                getTranslation("jobs.subtitle"));
        header.setToolbar(toolbar);
        VerticalLayout gridCard = new VerticalLayout(grid);
        ConsoleStyles.applyContentCard(gridCard);
        add(header, gridCard);
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
        grid.addColumn(TaskExecutionSummary::instanceId).setHeader(getTranslation("jobs.col.instance")).setSortable(true);
        grid.addComponentColumn(row -> {
            String label = row.templateName() != null ? row.templateName() : "—";
            if (row.templateId() == null) {
                return new Span(label);
            }
            Button link = new Button(label, e -> getUI().ifPresent(
                    ui -> ui.navigate(TemplateEditorView.class, String.valueOf(row.templateId()))));
            link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            return link;
        }).setHeader(getTranslation("jobs.col.template"));
        grid.addColumn(TaskExecutionSummary::definitionKind).setHeader(getTranslation("jobs.col.kind"));
        grid.addColumn(TaskExecutionSummary::status).setHeader(getTranslation("jobs.col.status"));
        grid.addColumn(TaskExecutionSummary::finishedAt).setHeader(getTranslation("jobs.col.finished"));
        grid.addComponentColumn(row -> {
            Button detail = new Button(getTranslation("common.detail"), e -> getUI().ifPresent(
                    ui -> ui.navigate(JobDetailView.class, String.valueOf(row.instanceId()))));
            detail.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            return detail;
        }).setHeader(getTranslation("common.detail"));
        grid.setSizeFull();
        ViewPageHeader.applyGridEmptyState(grid, getTranslation("grid.empty"));
    }

    private void refreshGrid() {
        try {
            Long templateId = parseTemplateIdFilter();
            List<TaskExecutionSummary> items = taskExecutionService.list(templateId);
            grid.setItems(items);
            updatePolling(items);
        } catch (NumberFormatException ex) {
            Notification.show(getTranslation("jobs.filter.invalid"));
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
                pollHint.setText(getTranslation("jobs.poll.active"));
            } else {
                ui.setPollInterval(-1);
                pollHint.setText(getTranslation("jobs.poll.idle"));
            }
        });
    }

    private static boolean isActive(TaskExecutionSummary row) {
        String status = row.status();
        return TaskExecutionStatus.QUEUED.name().equals(status)
                || TaskExecutionStatus.RUNNING.name().equals(status);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getUI().ifPresent(ui -> ui.getPage().setTitle(getTranslation("page.jobs")));
    }
}
