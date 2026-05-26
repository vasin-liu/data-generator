/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Clickable navigation tile for the home dashboard.
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
public class NavCard extends Div {

    /**
     * @param icon        Vaadin icon
     * @param title       card title
     * @param description short help text
     * @param routeClass  target {@link Route} view class
     */
    public NavCard(VaadinIcon icon, String title, String description, Class<? extends Component> routeClass) {
        addClassName(ConsoleStyles.NAV_CARD);
        Icon iconComponent = icon.create();
        iconComponent.addClassName("dg-nav-card-icon");
        iconComponent.setSize("28px");
        H3 heading = new H3(title);
        heading.addClassName("dg-nav-card-title");
        Paragraph detail = new Paragraph(description);
        detail.addClassName("dg-nav-card-desc");
        add(iconComponent, heading, detail);
        addClickListener(e -> UI.getCurrent().navigate(routeClass));
        // Keyboard-friendly focus ring via Lumo
        getElement().setAttribute("role", "link");
        getElement().setAttribute("tabindex", "0");
        String path = RouteConfiguration.forApplicationScope().getUrl(routeClass);
        getElement().setAttribute("aria-label", title + " — " + path);
    }
}
