/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import java.util.List;
import org.gensokyo.data.geo.GeoGenerationMode;
import org.gensokyo.data.geo.GeoGenerationRequest;
import org.gensokyo.data.geo.GeoSampleStrategyKind;
import org.gensokyo.data.model.v2.GeoSyntheticSampleVO;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link GeoSyntheticRequestMapper} four-mode mapping and invalid config (D-13).
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
class GeoSyntheticRequestMapperTests {

    private static final String SOURCE_NAME = "pts";

    @Test
    void boundaryPoints_mapsBoundaryPathCountAndSeed() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BOUNDARY_POINTS");
        source.setBoundaryPath("classpath:geo/南沙区边界.geojson");
        source.setCount(10);
        source.setSeed(42L);

        GeoGenerationRequest request = GeoSyntheticRequestMapper.toRequest(SOURCE_NAME, source);

        Assertions.assertEquals(GeoGenerationMode.BOUNDARY_POINTS, request.getMode());
        Assertions.assertEquals("classpath:geo/南沙区边界.geojson", request.getBoundaryPath());
        Assertions.assertEquals(10, request.getCount());
        Assertions.assertEquals(42L, request.getSeed());
        Assertions.assertDoesNotThrow(request::validate);
    }

    @Test
    void lineSample_mapsNetworkPathSampleStrategyAndSpacing() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("LINE_SAMPLE");
        source.setNetworkPath("classpath:geo/南沙区道路路网.geojson");
        GeoSyntheticSampleVO sample = new GeoSyntheticSampleVO();
        // BY_SPACING_METERS yields evenly spaced points along the line network.
        sample.setStrategy("BY_SPACING_METERS");
        sample.setSpacingMeters(50d);
        source.setSample(sample);

        GeoGenerationRequest request = GeoSyntheticRequestMapper.toRequest(SOURCE_NAME, source);

        Assertions.assertEquals(GeoGenerationMode.LINE_SAMPLE, request.getMode());
        Assertions.assertEquals("classpath:geo/南沙区道路路网.geojson", request.getNetworkPath());
        Assertions.assertEquals(GeoSampleStrategyKind.BY_SPACING_METERS, request.getSampleStrategy());
        Assertions.assertEquals(50d, request.getSpacingMeters());
        Assertions.assertDoesNotThrow(request::validate);
    }

    @Test
    void bbox_expandsBboxArrayToFlatFields() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BBOX");
        source.setBbox(List.of(113.2d, 23.0d, 113.5d, 23.2d));
        source.setCount(5);
        source.setSeed(1L);

        GeoGenerationRequest request = GeoSyntheticRequestMapper.toRequest(SOURCE_NAME, source);

        Assertions.assertEquals(113.2d, request.getBboxMinLon());
        Assertions.assertEquals(23.0d, request.getBboxMinLat());
        Assertions.assertEquals(113.5d, request.getBboxMaxLon());
        Assertions.assertEquals(23.2d, request.getBboxMaxLat());
        Assertions.assertDoesNotThrow(request::validate);
    }

    @Test
    void circle_expandsCenterArrayAndRadius() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("CIRCLE");
        source.setCenter(List.of(113.3d, 23.1d));
        source.setRadiusMeters(500d);
        source.setCount(3);
        source.setSeed(7L);

        GeoGenerationRequest request = GeoSyntheticRequestMapper.toRequest(SOURCE_NAME, source);

        Assertions.assertEquals(113.3d, request.getCenterLon());
        Assertions.assertEquals(23.1d, request.getCenterLat());
        Assertions.assertEquals(500d, request.getRadiusMeters());
        Assertions.assertDoesNotThrow(request::validate);
    }

    @Test
    void blankBoundaryPath_throwsWithSourceNameAndField() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BOUNDARY_POINTS");
        source.setBoundaryPath("  ");
        source.setCount(1);

        IllegalArgumentException error = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeoSyntheticRequestMapper.toRequest(SOURCE_NAME, source));

        Assertions.assertTrue(error.getMessage().contains(SOURCE_NAME));
        Assertions.assertTrue(error.getMessage().contains("boundaryPath"));
    }

    @Test
    void invalidBboxSize_throwsWithSourceNameAndField() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BBOX");
        source.setBbox(List.of(113.2d, 23.0d, 113.5d));
        source.setCount(1);

        IllegalArgumentException error = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeoSyntheticRequestMapper.toRequest(SOURCE_NAME, source));

        Assertions.assertTrue(error.getMessage().contains(SOURCE_NAME));
        Assertions.assertTrue(error.getMessage().contains("bbox"));
    }

    @Test
    void circleZeroRadius_surfacesValidateErrorThroughMapper() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("CIRCLE");
        source.setCenter(List.of(113.3d, 23.1d));
        source.setRadiusMeters(0d);
        source.setCount(1);

        IllegalArgumentException error = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeoSyntheticRequestMapper.toRequest(SOURCE_NAME, source));

        Assertions.assertTrue(error.getMessage().contains(SOURCE_NAME));
        Assertions.assertTrue(error.getMessage().contains("radiusMeters"));
    }
}
