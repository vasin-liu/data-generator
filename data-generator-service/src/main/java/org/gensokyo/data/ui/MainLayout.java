/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.ui.datasource.DataSourceListView;
import org.gensokyo.data.ui.job.JobListView;
import org.gensokyo.data.ui.migration.MigrationDashboardView;
import org.gensokyo.data.ui.template.TemplateListView;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Root layout for the operator console: navigation drawer and runtime status banner.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class MainLayout extends AppLayout implements RouterLayout {

    private final DataGeneratorProperties properties;

    /**
     * Creates the layout with navigation and V1 execution flag banner.
     *
     * @param properties application configuration
     */
    @Autowired
    public MainLayout(DataGeneratorProperties properties) {
        this.properties = properties;
        setPrimarySection(Section.DRAWER);
        addToNavbar(createNavbar());
        addToDrawer(createDrawer());
    }

    private HorizontalLayout createNavbar() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("Data Generator");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Span v1Flag = new Span(v1ExecutionBannerText());
        v1Flag.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        HorizontalLayout header = new HorizontalLayout(toggle, title, v1Flag);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        return header;
    }

    private VerticalLayout createDrawer() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Home", HomeView.class));
        nav.addItem(new SideNavItem("Templates", TemplateListView.class));
        nav.addItem(new SideNavItem("Datasources", DataSourceListView.class));
        nav.addItem(new SideNavItem("Jobs", JobListView.class));
        nav.addItem(new SideNavItem("Migration", MigrationDashboardView.class));
        VerticalLayout drawer = new VerticalLayout(nav);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        return drawer;
    }

    private String v1ExecutionBannerText() {
        boolean enabled = properties.isV1ExecutionEnabled();
        return enabled ? "V1 execution: enabled" : "V1 execution: disabled (V2 only)";
    }
}
