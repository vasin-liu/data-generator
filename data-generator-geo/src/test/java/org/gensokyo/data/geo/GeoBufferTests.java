/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GeoBuffer}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class GeoBufferTests {

    @Test
    void bufferWktExpandsPointToPolygonContainingOriginal() {
        String point = "POINT(113.2 22.2)";
        String buffered = GeoBuffer.bufferWkt(point, 500);
        Assertions.assertTrue(buffered.startsWith("POLYGON"));
        Assertions.assertTrue(GeoWktPredicates.contains(buffered, point));
    }

    @Test
    void bufferGeoJsonReturnsPolygonGeometry() {
        String point = "{\"type\":\"Point\",\"coordinates\":[113.2,22.2]}";
        String buffered = GeoBuffer.bufferGeoJson(point, 500);
        Assertions.assertTrue(buffered.contains("\"Polygon\""));
        Assertions.assertTrue(GeoJsonPredicates.contains(buffered, point));
    }

    @Test
    void rejectsNegativeDistance() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> GeoBuffer.bufferWkt("POINT(0 0)", -1));
    }
}
