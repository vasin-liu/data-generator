/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.format;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * Tests for {@link GeoJsonGeometryEncoder}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class GeoJsonGeometryEncoderTests {

    private static final GeometryFactory GF = new GeometryFactory();

    @Test
    void encodesPointWithTwoDimensions() {
        Point point = GF.createPoint(new Coordinate(113.5, 22.8));
        String json = GeoJsonGeometryEncoder.encode(point);
        Assertions.assertTrue(json.contains("\"type\":\"Point\""));
        Assertions.assertTrue(json.contains("[113.5,22.8]"));
    }
}
