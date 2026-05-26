/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Consistent page title and optional subtitle for operator console views.
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
public class ViewPageHeader extends VerticalLayout {

    private final H2 heading;

    /**
     * @param title    page heading
     * @param subtitle optional description; may be null
     */
    public ViewPageHeader(String title, String subtitle) {
        addClassName(ConsoleStyles.PAGE_HEADER);
        setPadding(false);
        setSpacing(false);
        heading = new H2(title);
        heading.addClassName(ConsoleStyles.PAGE_TITLE);
        add(heading);
        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph detail = new Paragraph(subtitle);
            detail.addClassName(ConsoleStyles.PAGE_SUBTITLE);
            add(detail);
        }
    }

    /**
     * Updates the visible page heading (e.g. after async load).
     *
     * @param title new title text
     */
    public void setTitle(String title) {
        heading.setText(title);
    }

    /**
     * Adds an optional toolbar row below the subtitle.
     *
     * @param toolbar actions (filters, buttons)
     */
    public void setToolbar(Component toolbar) {
        add(toolbar);
    }

    /**
     * Shows a friendly message when a data grid has no rows.
     *
     * @param grid    target grid
     * @param message empty-state text
     */
    public static void applyGridEmptyState(Grid<?> grid, String message) {
        Span empty = new Span(message);
        empty.addClassName(ConsoleStyles.PAGE_SUBTITLE);
        grid.setEmptyStateComponent(empty);
    }
}
