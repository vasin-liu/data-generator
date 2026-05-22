/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.job;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.PlaceholderPanel;

/**
 * Task execution history and live status (P3 will replace placeholder).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "jobs", layout = MainLayout.class)
@PageTitle("Jobs | Data Generator")
public class JobListView extends VerticalLayout {

    /**
     * Placeholder until P3 task_execution UI is implemented.
     */
    public JobListView() {
        setSizeFull();
        add(new PlaceholderPanel(
                "Jobs",
                "P3: Run templates, poll RUNNING status, browse task_execution history."));
    }
}
