/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.datasource;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.PlaceholderPanel;

/**
 * JDBC datasource administration (P2 will replace placeholder).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "datasources", layout = MainLayout.class)
@PageTitle("Datasources | Data Generator")
public class DataSourceListView extends VerticalLayout {

    /**
     * Placeholder until P2 datasource_config CRUD UI is implemented.
     */
    public DataSourceListView() {
        setSizeFull();
        add(new PlaceholderPanel(
                "Datasources",
                "P2: datasource_config table (D1), connection test, driver upload. Persists across restart."));
    }
}
