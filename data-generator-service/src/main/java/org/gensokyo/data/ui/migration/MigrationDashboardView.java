/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.migration;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.PlaceholderPanel;

/**
 * V1 to V2 migration inventory dashboard (P4 will replace placeholder).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Route(value = "migration", layout = MainLayout.class)
@PageTitle("Migration | Data Generator")
public class MigrationDashboardView extends VerticalLayout {

    /**
     * Placeholder until P4 migration summary/backlog UI is implemented.
     */
    public MigrationDashboardView() {
        setSizeFull();
        add(new PlaceholderPanel(
                "Migration",
                "P4: Summary, backlog grid, per-template analyze/compare/promote tab. "
                        + "W3 COMPATIBILITY_ONLY templates cannot promote (policy S1)."));
    }
}
