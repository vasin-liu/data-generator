/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.faker.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Loads bundled boundary GeoJSON from the classpath and generates sample points.
 *
 * @author Gensokyo V.L.
 * @since 2025/4/8 , Version 1.0.0
 */
class GeoKitTests {

    @Test
    void generatesPointsInsideBundledBoundary() throws Exception {
        URL resource = GeoKitTests.class.getResource("/geo/南沙区边界.geojson");
        Assertions.assertNotNull(resource, "Missing test resource /geo/南沙区边界.geojson");
        Path geoJsonPath = Paths.get(resource.toURI());

        List<Point> randomPoints = GeoKit.generateRandomPointsFromGeoJson(
                geoJsonPath,
                0,
                100,
                50,
                2024L);

        Assertions.assertEquals(100, randomPoints.size());
    }
}
