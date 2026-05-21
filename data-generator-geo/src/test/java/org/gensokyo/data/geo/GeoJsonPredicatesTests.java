/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GeoJsonPredicates}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class GeoJsonPredicatesTests {

    private static final String BOX = """
            {"type":"Polygon","coordinates":[[[113.15,22.15],[113.25,22.15],[113.25,22.25],[113.15,22.25],[113.15,22.15]]]}
            """;

    @Test
    void pointInGeoJsonRespectsBoundingPolygon() {
        Assertions.assertTrue(GeoJsonPredicates.pointInGeoJson(22.2, 113.2, BOX));
        Assertions.assertFalse(GeoJsonPredicates.pointInGeoJson(22.1, 113.1, BOX));
    }

    @Test
    void containsPolygonCoversPointGeoJson() {
        String inner = "{\"type\":\"Point\",\"coordinates\":[113.2,22.2]}";
        Assertions.assertTrue(GeoJsonPredicates.contains(BOX, inner));
        Assertions.assertFalse(GeoJsonPredicates.contains(BOX, "{\"type\":\"Point\",\"coordinates\":[113.1,22.1]}"));
    }

    @Test
    void intersectsForOverlappingPoints() {
        String point = "{\"type\":\"Point\",\"coordinates\":[113.2,22.2]}";
        Assertions.assertTrue(GeoJsonPredicates.intersects(point, point));
    }
}
