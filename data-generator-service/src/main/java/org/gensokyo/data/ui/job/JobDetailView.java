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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Single execution detail with auto-refresh while QUEUED or RUNNING.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "jobs/:instanceId", layout = MainLayout.class)
@PageTitle("Job detail | Data Generator")
public class JobDetailView extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private static final int POLL_MS = 2000;
    private static final DateTimeFormatter INSTANT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final TaskExecutionService taskExecutionService;
    private ViewPageHeader pageHeader;
    private final Span statusLine = new Span();
    private final Span timingLine = new Span();
    private final Pre metricsBlock = new Pre();
    private final Button openTemplate = new Button();
    private Long instanceId;
    private Long linkedTemplateId;
    private Registration pollRegistration;

    /**
     * @param taskExecutionService execution lookup
     */
    @Autowired
    public JobDetailView(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
        ConsoleStyles.applyPage(this);
        setSizeFull();
        timingLine.addClassName(ConsoleStyles.PAGE_SUBTITLE);
        metricsBlock.setWidthFull();
        metricsBlock.getStyle().set("white-space", "pre-wrap");
        H3 metricsTitle = new H3(getTranslation("jobDetail.metrics"));
        metricsTitle.addClassName(ConsoleStyles.SECTION_TITLE);
        VerticalLayout metricsCard = new VerticalLayout(metricsTitle, metricsBlock);
        ConsoleStyles.applyContentCard(metricsCard);
        pageHeader = buildHeader();
        add(pageHeader, statusLine, timingLine, metricsCard);
    }

    private ViewPageHeader buildHeader() {
        Button back = new Button(getTranslation("jobDetail.back"), e ->
                getUI().ifPresent(ui -> ui.navigate(JobListView.class)));
        back.setIcon(VaadinIcon.ARROW_BACKWARD.create());
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button refresh = new Button(getTranslation("jobDetail.refresh"), e -> load());
        refresh.setIcon(VaadinIcon.REFRESH.create());
        openTemplate.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        openTemplate.setIcon(VaadinIcon.FILE_TEXT.create());
        openTemplate.setVisible(false);
        openTemplate.addClickListener(e -> {
            if (linkedTemplateId != null) {
                getUI().ifPresent(ui -> ui.navigate(TemplateEditorView.class, String.valueOf(linkedTemplateId)));
            }
        });
        HorizontalLayout toolbar = new HorizontalLayout(back, refresh, openTemplate);
        toolbar.setAlignItems(Alignment.CENTER);
        ConsoleStyles.applyToolbar(toolbar);
        ViewPageHeader header = new ViewPageHeader(
                getTranslation("jobDetail.title"),
                null);
        header.setToolbar(toolbar);
        return header;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        if (ui != null) {
            pollRegistration = ui.addPollListener(event -> load());
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

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        if (parameter != null && !parameter.isBlank()) {
            instanceId = Long.parseLong(parameter);
        }
        load();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getUI().ifPresent(ui -> {
            if (instanceId != null) {
                ui.getPage().setTitle(getTranslation("jobDetail.titleWithId", instanceId)
                        + " | Data Generator");
            } else {
                ui.getPage().setTitle(getTranslation("page.jobDetail"));
            }
        });
        applyI18n();
    }

    private void applyI18n() {
        openTemplate.setText(getTranslation("jobDetail.openTemplate"));
    }

    private void load() {
        if (instanceId == null) {
            statusLine.setText(getTranslation("jobDetail.missingId"));
            timingLine.setText("");
            metricsBlock.setText("");
            openTemplate.setVisible(false);
            updatePolling(false);
            return;
        }
        try {
            TaskExecutionSummary row = taskExecutionService.getByInstanceId(instanceId);
            pageHeader.setTitle(getTranslation("jobDetail.titleWithId", instanceId));
            statusLine.setText(getTranslation(
                    "jobDetail.statusLine",
                    row.templateName(),
                    row.templateId(),
                    row.status(),
                    row.definitionKind(),
                    row.rowCount() != null ? row.rowCount() : "—"));
            timingLine.setText(formatTiming(row));
            String body = row.metricsJson() != null && !row.metricsJson().isBlank()
                    ? row.metricsJson()
                    : (row.errorMessage() != null ? row.errorMessage() : "");
            metricsBlock.setText(body);
            linkedTemplateId = row.templateId();
            openTemplate.setVisible(linkedTemplateId != null);
            updatePolling(isActive(row.status()));
        } catch (Exception ex) {
            Notification.show(getTranslation("jobDetail.load.failed", ex.getMessage()));
            updatePolling(false);
        }
    }

    private String formatTiming(TaskExecutionSummary row) {
        String queued = row.queuedAt() != null ? INSTANT_FMT.format(row.queuedAt()) : "—";
        String started = row.startedAt() != null ? INSTANT_FMT.format(row.startedAt()) : "—";
        String finished = row.finishedAt() != null ? INSTANT_FMT.format(row.finishedAt()) : "—";
        return getTranslation("jobDetail.queued") + ": " + queued + "  |  "
                + getTranslation("jobDetail.started") + ": " + started + "  |  "
                + getTranslation("jobDetail.finished") + ": " + finished;
    }

    private void updatePolling(boolean active) {
        getUI().ifPresent(ui -> ui.setPollInterval(active ? POLL_MS : -1));
    }

    private static boolean isActive(String status) {
        return TaskExecutionStatus.QUEUED.name().equals(status)
                || TaskExecutionStatus.RUNNING.name().equals(status);
    }
}
