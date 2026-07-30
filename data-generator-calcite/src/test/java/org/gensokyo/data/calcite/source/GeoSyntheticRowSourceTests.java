/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import java.util.List;
import org.gensokyo.data.model.v2.GeoSyntheticSampleVO;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Four-mode RowSource materialization coverage for Template V2 {@code geo_synthetic} (D-13).
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
class GeoSyntheticRowSourceTests {

    private static final String BOUNDARY_FIXTURE = "classpath:geo/南沙区边界.geojson";
    private static final String NETWORK_FIXTURE = "classpath:geo/南沙区道路路网.geojson";

    @Test
    void boundaryPoints_materializesCountAndLonLatSchema() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BOUNDARY_POINTS");
        source.setBoundaryPath(BOUNDARY_FIXTURE);
        source.setCount(6);
        source.setSeed(11L);

        GeoSyntheticRowSource rowSource = new GeoSyntheticRowSource("boundary_src", source);

        Assertions.assertEquals(6, rowSource.rows().size());
        Assertions.assertTrue(rowSource.schema().getColumns().stream().anyMatch(c -> "lon".equals(c.getName())));
        Assertions.assertTrue(rowSource.schema().getColumns().stream().anyMatch(c -> "lat".equals(c.getName())));
    }

    @Test
    void lineSample_materializesNonEmptyRowsAndSchema() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("LINE_SAMPLE");
        source.setNetworkPath(NETWORK_FIXTURE);
        GeoSyntheticSampleVO sample = new GeoSyntheticSampleVO();
        sample.setStrategy("BY_SPACING_METERS");
        sample.setSpacingMeters(50d);
        source.setSample(sample);

        GeoSyntheticRowSource rowSource = new GeoSyntheticRowSource("line_src", source);

        Assertions.assertFalse(rowSource.rows().isEmpty());
        Assertions.assertFalse(rowSource.schema().getColumns().isEmpty());
    }

    @Test
    void bbox_materializesExactCount() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BBOX");
        source.setBbox(List.of(113.2d, 23.0d, 113.5d, 23.2d));
        source.setCount(5);
        source.setSeed(42L);

        GeoSyntheticRowSource rowSource = new GeoSyntheticRowSource("bbox_src", source);

        Assertions.assertEquals(5, rowSource.rows().size());
    }

    @Test
    void circle_materializesExactCount() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("CIRCLE");
        source.setCenter(List.of(113.3d, 23.1d));
        source.setRadiusMeters(500d);
        source.setCount(4);
        source.setSeed(7L);

        GeoSyntheticRowSource rowSource = new GeoSyntheticRowSource("circle_src", source);

        Assertions.assertEquals(4, rowSource.rows().size());
    }

    @Test
    void invalidPathFailsWithSourceAndPath() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BOUNDARY_POINTS");
        source.setBoundaryPath("classpath:geo/missing.geojson");
        source.setCount(1);

        IllegalArgumentException error = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new GeoSyntheticRowSource("missing_src", source));

        Assertions.assertTrue(error.getMessage().contains("missing_src"));
        Assertions.assertTrue(error.getMessage().contains("classpath:geo/missing.geojson"));
    }
}
