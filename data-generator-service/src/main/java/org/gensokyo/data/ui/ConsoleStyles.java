/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * CSS class names for the operator console theme ({@code themes/data-generator/styles.css}).
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
public final class ConsoleStyles {

    public static final String LAYOUT = "console-layout";
    public static final String NAVBAR_TITLE = "console-navbar-title";
    public static final String STATUS_BADGE = "console-status-badge";
    public static final String DRAWER_HEADER = "console-drawer-header";
    public static final String SIDE_NAV = "console-nav";

    public static final String PAGE = "dg-page";
    public static final String PAGE_HEADER = "dg-page-header";
    public static final String PAGE_TITLE = "dg-page-title";
    public static final String PAGE_SUBTITLE = "dg-page-subtitle";
    public static final String TOOLBAR = "dg-toolbar";
    public static final String CONTENT_CARD = "dg-content-card";
    public static final String STAT_GRID = "dg-stat-grid";
    public static final String STAT_CARD = "dg-stat-card";
    public static final String STAT_LABEL = "dg-stat-label";
    public static final String STAT_VALUE = "dg-stat-value";
    public static final String NAV_GRID = "dg-nav-grid";
    public static final String NAV_CARD = "dg-nav-card";
    public static final String HERO_CARD = "dg-hero-card";
    public static final String SECTION_TITLE = "dg-section-title";
    public static final String EDITOR_CARD = "dg-editor-card";
    public static final String EDITOR_TITLE = "dg-editor-title";
    public static final String EDITOR_STEP = "dg-editor-step";
    public static final String EDITOR_BACK = "dg-editor-back";
    public static final String YAML_PANEL = "dg-yaml-panel";
    public static final String GRID_LINK = "dg-grid-link";

    private ConsoleStyles() {
    }

    /**
     * Applies standard page layout classes and spacing.
     *
     * @param page root vertical layout for a route view
     */
    public static void applyPage(VerticalLayout page) {
        page.addClassName(PAGE);
        page.setPadding(true);
        page.setSpacing(true);
    }

    /**
     * Styles a horizontal toolbar strip.
     *
     * @param toolbar action row
     */
    public static void applyToolbar(HorizontalLayout toolbar) {
        toolbar.addClassName(TOOLBAR);
        toolbar.setWidthFull();
    }

    /**
     * Wraps main content (typically a grid) in a card surface.
     *
     * @param card content container
     */
    public static void applyContentCard(VerticalLayout card) {
        card.addClassName(CONTENT_CARD);
        card.setPadding(true);
        card.setSpacing(true);
        card.setSizeFull();
    }

    /**
     * Tags the root app layout for navbar/drawer theming.
     *
     * @param layout main shell
     */
    public static void applyShell(AppLayout layout) {
        layout.addClassName(LAYOUT);
    }
}
