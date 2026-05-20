/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.geo.generate.LineComponentSelector;
import org.gensokyo.data.geo.io.GeoFeature;
import org.gensokyo.data.geo.io.GeoJsonLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.util.List;
import java.util.Map;

/**
 * Integration tests for {@link GeoSyntheticGenerator} with bundled fixtures.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class GeoSyntheticGeneratorTests {

    private static final String BOUNDARY = "classpath:geo/南沙区边界.geojson";
    private static final String ROADS = "classpath:geo/南沙区道路路网.geojson";

    @Test
    void boundaryPointsProducesRequestedCount() throws Exception {
        GeoGenerationRequest request = new GeoGenerationRequest();
        request.setMode(GeoGenerationMode.BOUNDARY_POINTS);
        request.setBoundaryPath(BOUNDARY);
        request.setFeatureIndex(0);
        request.setCount(25);
        request.setSeed(7L);
        request.setMinDistanceMeters(0d);
        request.setOutputFormat(GeoOutputFormatKind.columns);

        List<Map<String, Object>> rows = GeoSyntheticGenerator.generateRows(request);
        Assertions.assertEquals(25, rows.size());
        Assertions.assertTrue(rows.get(0).containsKey("lat"));
        Assertions.assertTrue(rows.get(0).containsKey("lon"));
    }

    @Test
    void lineSampleByCountKeepsPointsNearNetwork() throws Exception {
        GeoGenerationRequest request = new GeoGenerationRequest();
        request.setMode(GeoGenerationMode.LINE_SAMPLE);
        request.setNetworkPath(ROADS);
        request.setFeatureIndex(0);
        request.setRandomFeature(false);
        request.setSampleStrategy(GeoSampleStrategyKind.BY_COUNT);
        request.setCount(20);
        request.setSeed(3L);
        request.setOutputFormat(GeoOutputFormatKind.columns);

        GeoFeature feature = GeoJsonLoader.loadFeature(ROADS, 0, false, 3L);
        LineString line = LineComponentSelector.selectLongestLineString(feature.geometry());
        List<Point> points = GeoSyntheticGenerator.generatePoints(request);

        for (Point point : points) {
            double distanceDegrees = new DistanceOp(line, point).distance();
            Assertions.assertTrue(distanceDegrees < 5e-5, "Point too far from sampled line");
        }
    }
}
