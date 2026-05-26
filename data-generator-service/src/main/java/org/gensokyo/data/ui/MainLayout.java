/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.ui.datasource.DataSourceListView;
import org.gensokyo.data.ui.i18n.ConsoleI18n;
import org.gensokyo.data.ui.i18n.ConsoleSessionPreferences;
import org.gensokyo.data.ui.job.JobListView;
import org.gensokyo.data.ui.migration.MigrationDashboardView;
import org.gensokyo.data.ui.template.TemplateListView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;

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
        ConsoleStyles.applyShell(this);
        setPrimarySection(Section.DRAWER);
        addToNavbar(createNavbar());
        addToDrawer(createDrawer());
    }

    private HorizontalLayout createNavbar() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1(getTranslation(ConsoleI18n.Key.APP_TITLE));
        title.addClassNames(ConsoleStyles.NAVBAR_TITLE, LumoUtility.Margin.NONE);
        Span v1Flag = new Span(v1ExecutionBannerText());
        v1Flag.addClassName(ConsoleStyles.STATUS_BADGE);

        ComboBox<Locale> localeBox = new ComboBox<>(getTranslation(ConsoleI18n.Key.PREFS_LOCALE));
        localeBox.setItems(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH);
        localeBox.setItemLabelGenerator(this::localeLabel);
        localeBox.setValue(ConsoleSessionPreferences.currentLocale());
        localeBox.setWidth("8rem");
        localeBox.addValueChangeListener(e -> {
            if (!e.isFromClient() || e.getValue() == null) {
                return;
            }
            Locale selected = e.getValue();
            if (!selected.equals(ConsoleSessionPreferences.currentLocale())) {
                ConsoleSessionPreferences.setLocale(selected);
            }
        });

        com.vaadin.flow.component.checkbox.Checkbox darkMode =
                new com.vaadin.flow.component.checkbox.Checkbox(getTranslation(ConsoleI18n.Key.PREFS_DARK));
        darkMode.setValue(ConsoleSessionPreferences.isDarkMode());
        darkMode.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                ConsoleSessionPreferences.setDarkMode(Boolean.TRUE.equals(e.getValue()));
            }
        });

        HorizontalLayout prefs = new HorizontalLayout(localeBox, darkMode);
        prefs.setAlignItems(FlexComponent.Alignment.CENTER);
        prefs.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(toggle, title, v1Flag, prefs);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        return header;
    }

    private String localeLabel(Locale locale) {
        if (Locale.SIMPLIFIED_CHINESE.equals(locale)) {
            return getTranslation(ConsoleI18n.Key.PREFS_LANG_ZH);
        }
        return getTranslation(ConsoleI18n.Key.PREFS_LANG_EN);
    }

    private VerticalLayout createDrawer() {
        Span drawerTitle = new Span(getTranslation(ConsoleI18n.Key.NAV_DRAWER));
        drawerTitle.addClassName(ConsoleStyles.DRAWER_HEADER);
        SideNav nav = new SideNav();
        nav.addClassName(ConsoleStyles.SIDE_NAV);
        nav.addItem(new SideNavItem(getTranslation(ConsoleI18n.Key.NAV_HOME), HomeView.class, VaadinIcon.HOME.create()));
        nav.addItem(new SideNavItem(
                getTranslation(ConsoleI18n.Key.NAV_TEMPLATES), TemplateListView.class, VaadinIcon.FILE_TEXT.create()));
        nav.addItem(new SideNavItem(
                getTranslation(ConsoleI18n.Key.NAV_DATASOURCES),
                DataSourceListView.class,
                VaadinIcon.DATABASE.create()));
        nav.addItem(new SideNavItem(getTranslation(ConsoleI18n.Key.NAV_JOBS), JobListView.class, VaadinIcon.CLOCK.create()));
        nav.addItem(new SideNavItem(
                getTranslation(ConsoleI18n.Key.NAV_MIGRATION),
                MigrationDashboardView.class,
                VaadinIcon.EXCHANGE.create()));
        VerticalLayout drawer = new VerticalLayout(drawerTitle, nav);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        return drawer;
    }

    private String v1ExecutionBannerText() {
        boolean enabled = properties.isV1ExecutionEnabled();
        return enabled ? getTranslation(ConsoleI18n.Key.V1_ENABLED) : getTranslation(ConsoleI18n.Key.V1_DISABLED);
    }
}
