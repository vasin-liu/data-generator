/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

/**
 * Global Vaadin theme and PWA metadata for the operator console.
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
@Theme(value = "data-generator")
@PWA(name = "Data Generator", shortName = "DataGen")
@Push
public class AppShell implements AppShellConfigurator {
}
