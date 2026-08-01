/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

/**
 * One template that references a geo asset (delete guard scan result per D-08).
 *
 * @param templateId   catalog template primary key
 * @param templateName operator-visible template name
 * @author Gensokyo
 * @since 2026-08-01
 */
public record GeoAssetTemplateUsage(Long templateId, String templateName) {
}
