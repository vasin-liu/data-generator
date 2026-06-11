/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.io;

import org.locationtech.jts.geom.Geometry;

import java.util.Map;

/**
 * One GeoJSON feature geometry and optional properties.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public record GeoFeature(Geometry geometry, Map<String, Object> properties) {
}
