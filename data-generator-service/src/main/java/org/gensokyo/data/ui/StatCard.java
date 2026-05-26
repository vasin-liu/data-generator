/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * KPI-style metric tile for dashboards.
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
public class StatCard extends Div {

    /**
     * @param label metric name
     * @param value numeric value to display
     */
    public StatCard(String label, int value) {
        addClassName(ConsoleStyles.STAT_CARD);
        Span labelSpan = new Span(label);
        labelSpan.addClassName(ConsoleStyles.STAT_LABEL);
        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.addClassName(ConsoleStyles.STAT_VALUE);
        add(labelSpan, valueSpan);
    }
}
