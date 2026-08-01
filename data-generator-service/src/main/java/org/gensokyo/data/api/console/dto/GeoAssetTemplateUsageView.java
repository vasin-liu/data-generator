/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.geo.GeoAssetTemplateUsage;

/**
 * Console DTO for template usage hints on geo asset delete conflict (409 per D-08).
 *
 * @param templateId   catalog template primary key
 * @param templateName operator-visible template name
 * @author Gensokyo
 * @since 2026-08-01
 */
public record GeoAssetTemplateUsageView(Long templateId, String templateName) {

    /**
     * Maps a scan hit to the console view.
     *
     * @param usage domain usage record
     * @return API view
     */
    public static GeoAssetTemplateUsageView from(GeoAssetTemplateUsage usage) {
        return new GeoAssetTemplateUsageView(usage.templateId(), usage.templateName());
    }
}
