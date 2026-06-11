/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GeoWktPredicates}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class GeoWktPredicatesTests {

    private static final String BOX =
            "POLYGON((113.15 22.15, 113.25 22.15, 113.25 22.25, 113.15 22.25, 113.15 22.15))";

    @Test
    void pointInWktRespectsBoundingPolygon() {
        Assertions.assertTrue(GeoWktPredicates.pointInWkt(22.2, 113.2, BOX));
        Assertions.assertFalse(GeoWktPredicates.pointInWkt(22.1, 113.1, BOX));
    }

    @Test
    void containsPolygonCoversPointWkt() {
        Assertions.assertTrue(GeoWktPredicates.contains(BOX, "POINT(113.2 22.2)"));
        Assertions.assertFalse(GeoWktPredicates.contains(BOX, "POINT(113.1 22.1)"));
    }

    @Test
    void intersectsForOverlappingPoints() {
        Assertions.assertTrue(GeoWktPredicates.intersects("POINT(113.2 22.2)", "POINT(113.2 22.2)"));
        Assertions.assertFalse(GeoWktPredicates.intersects("POINT(113.1 22.1)", "POINT(113.5 22.5)"));
    }

    @Test
    void rejectsBlankWkt() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> GeoWktPredicates.parse("  "));
    }
}
