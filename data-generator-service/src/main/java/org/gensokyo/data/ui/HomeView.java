/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * Operator console landing page with links to primary work areas.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home | Data Generator")
public class HomeView extends VerticalLayout {

    /**
     * Default constructor building welcome content.
     */
    public HomeView() {
        setPadding(true);
        add(new PlaceholderPanel(
                "Operator console",
                "Use the drawer to open Templates, Datasources, Jobs, or Migration. "
                        + "REST APIs remain available under /template, /task, and /datasource."));
        add(new RouterLink("Templates", org.gensokyo.data.ui.template.TemplateListView.class));
        add(new RouterLink("Datasources", org.gensokyo.data.ui.datasource.DataSourceListView.class));
        add(new RouterLink("Jobs", org.gensokyo.data.ui.job.JobListView.class));
        add(new RouterLink("Migration", org.gensokyo.data.ui.migration.MigrationDashboardView.class));
        add(new Anchor("/template/migration/summary", "Migration summary (REST)"));
    }
}
