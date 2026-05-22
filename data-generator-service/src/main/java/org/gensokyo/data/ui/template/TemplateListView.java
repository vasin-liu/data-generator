/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.PlaceholderPanel;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Template catalog and editor entry (P1 will replace placeholder).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "templates", layout = MainLayout.class)
@PageTitle("Templates | Data Generator")
public class TemplateListView extends VerticalLayout {

    /**
     * Placeholder until P1 list grid and editor routes are implemented.
     */
    public TemplateListView() {
        setSizeFull();
        add(new PlaceholderPanel(
                "Templates",
                "P1: V2 form wizard, YAML advanced mode, archive/restore. Planned route: templates/new, templates/{id}."));
    }
}
