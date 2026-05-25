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
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionStatus;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.gensokyo.data.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Single execution detail with auto-refresh while QUEUED or RUNNING.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@Route(value = "jobs/:instanceId", layout = MainLayout.class)
@PageTitle("Job detail | Data Generator")
public class JobDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private static final int POLL_MS = 2000;

    private final TaskExecutionService taskExecutionService;
    private final Span statusLine = new Span();
    private final Pre metricsBlock = new Pre();
    private Long instanceId;
    private Registration pollRegistration;

    /**
     * @param taskExecutionService execution lookup
     */
    @Autowired
    public JobDetailView(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
        setPadding(true);
        add(new H2("Job detail"), statusLine, metricsBlock, new Button("Refresh", e -> load()));
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
    public void setParameter(com.vaadin.flow.router.BeforeEvent event, String parameter) {
        if (parameter != null && !parameter.isBlank()) {
            instanceId = Long.parseLong(parameter);
        }
        load();
    }

    private void load() {
        if (instanceId == null) {
            statusLine.setText("Missing instance id");
            updatePolling(false);
            return;
        }
        try {
            TaskExecutionSummary row = taskExecutionService.getByInstanceId(instanceId);
            statusLine.setText(String.format(
                    "Template %s (%s) — %s [%s] rows=%s",
                    row.templateName(),
                    row.templateId(),
                    row.status(),
                    row.definitionKind(),
                    row.rowCount()));
            metricsBlock.setText(row.metricsJson() != null ? row.metricsJson() : row.errorMessage());
            updatePolling(isActive(row.status()));
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
            updatePolling(false);
        }
    }

    private void updatePolling(boolean active) {
        getUI().ifPresent(ui -> ui.setPollInterval(active ? POLL_MS : -1));
    }

    private static boolean isActive(String status) {
        return TaskExecutionStatus.QUEUED.name().equals(status)
                || TaskExecutionStatus.RUNNING.name().equals(status);
    }
}
