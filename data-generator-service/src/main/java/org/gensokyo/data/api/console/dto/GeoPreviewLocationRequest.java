/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Console request to resolve path/classpath/{@code asset:} GeoJSON for map underlays (D-06).
 *
 * @param location classpath, filesystem, or {@code asset:{uuid}} location
 * @author Gensokyo
 * @since 2026-08-06
 */
public record GeoPreviewLocationRequest(String location) {
}
