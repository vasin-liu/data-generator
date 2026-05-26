/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.ui.datasource.DataSourceListView;
import org.gensokyo.data.ui.job.JobListView;
import org.gensokyo.data.ui.migration.MigrationDashboardView;
import org.gensokyo.data.ui.template.TemplateListView;

/**
 * Operator console landing page with quick navigation cards.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home")
public class HomeView extends VerticalLayout implements AfterNavigationObserver {

    /**
     * Default constructor building welcome content.
     */
    public HomeView() {
        ConsoleStyles.applyPage(this);
        add(new PlaceholderPanel(
                getTranslation("home.hero.title"),
                getTranslation("home.hero.desc")));
        Div navGrid = new Div(
                new NavCard(
                        VaadinIcon.FILE_TEXT,
                        getTranslation("home.card.templates.title"),
                        getTranslation("home.card.templates.desc"),
                        TemplateListView.class),
                new NavCard(
                        VaadinIcon.DATABASE,
                        getTranslation("home.card.datasources.title"),
                        getTranslation("home.card.datasources.desc"),
                        DataSourceListView.class),
                new NavCard(
                        VaadinIcon.CLOCK,
                        getTranslation("home.card.jobs.title"),
                        getTranslation("home.card.jobs.desc"),
                        JobListView.class),
                new NavCard(
                        VaadinIcon.EXCHANGE,
                        getTranslation("home.card.migration.title"),
                        getTranslation("home.card.migration.desc"),
                        MigrationDashboardView.class));
        navGrid.addClassName(ConsoleStyles.NAV_GRID);
        add(navGrid);
        Anchor restLink = new Anchor("/template/migration/summary", getTranslation("home.rest.link"));
        restLink.addClassNames(ConsoleStyles.PAGE_SUBTITLE);
        restLink.setTarget("_blank");
        add(restLink);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getUI().ifPresent(ui -> ui.getPage().setTitle(getTranslation("page.home")));
    }
}
