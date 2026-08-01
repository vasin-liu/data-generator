/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link GeoJsonLocationMapper} asset-id normalization (GEO-10/D-01..D-03).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
class GeoJsonLocationMapperTests {

    private static final String SOURCE_NAME = "features";

    @Test
    void assetId_normalizesToAssetPrefix() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setAssetId("geo-789");

        String location = GeoJsonLocationMapper.resolveLocation(SOURCE_NAME, source);

        Assertions.assertEquals("asset:geo-789", location);
    }

    @Test
    void path_passthroughClasspath() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");

        String location = GeoJsonLocationMapper.resolveLocation(SOURCE_NAME, source);

        Assertions.assertEquals("classpath:geo/two_feature_collection.geojson", location);
    }

    @Test
    void pathAssetPrefix_passthroughWithoutDedicatedField() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("asset:wire-format-uuid");

        String location = GeoJsonLocationMapper.resolveLocation(SOURCE_NAME, source);

        Assertions.assertEquals("asset:wire-format-uuid", location);
    }

    @Test
    void pathAndAssetId_bothSet_throwsWithSourceAndFields() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");
        source.setAssetId("geo-789");

        IllegalArgumentException error = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeoJsonLocationMapper.resolveLocation(SOURCE_NAME, source));

        Assertions.assertTrue(error.getMessage().contains(SOURCE_NAME));
        Assertions.assertTrue(error.getMessage().contains("path"));
        Assertions.assertTrue(error.getMessage().contains("assetId"));
    }

    @Test
    void blankPathAndAssetId_throwsWithSourceName() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();

        IllegalArgumentException error = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeoJsonLocationMapper.resolveLocation(SOURCE_NAME, source));

        Assertions.assertTrue(error.getMessage().contains(SOURCE_NAME));
    }
}
