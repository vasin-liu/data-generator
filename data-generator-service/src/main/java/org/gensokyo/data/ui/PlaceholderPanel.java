/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Reusable placeholder body for console views not yet implemented in the current phase.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class PlaceholderPanel extends VerticalLayout {

    /**
     * Builds a titled placeholder with optional detail text.
     *
     * @param title       view title
     * @param description short implementation note
     */
    public PlaceholderPanel(String title, String description) {
        setPadding(true);
        setSpacing(true);
        H2 heading = new H2(title);
        heading.addClassNames(LumoUtility.Margin.Top.NONE);
        Paragraph detail = new Paragraph(description);
        detail.addClassNames(LumoUtility.TextColor.SECONDARY);
        add(heading, detail);
    }
}
