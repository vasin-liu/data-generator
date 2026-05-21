/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests for {@link GeoJsonLoader}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class GeoJsonLoaderTests {

    @Test
    void loadsPointGeometryFromFile() throws Exception {
        Path path = Files.createTempFile("gj-point", ".geojson");
        path.toFile().deleteOnExit();
        Files.writeString(path, "{\"type\":\"Point\",\"coordinates\":[113.5,22.8]}");

        Geometry geometry = GeoJsonLoader.loadGeometry(path.toAbsolutePath().toString(), 0);
        Assertions.assertInstanceOf(Point.class, geometry);
    }

    @Test
    void classpathResolverFailsOnMissingResource() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                GeoResourceResolver.readUtf8("classpath:missing-no-such-resource-789.json"));
    }

    @Test
    void loadFeatureCollectionFromClasspath() throws Exception {
        List<GeoFeature> features = GeoJsonLoader.loadFeatureCollection("classpath:geo/two_feature_collection.geojson");
        Assertions.assertEquals(2, features.size());
        Assertions.assertInstanceOf(Point.class, features.get(0).geometry());
    }
}
