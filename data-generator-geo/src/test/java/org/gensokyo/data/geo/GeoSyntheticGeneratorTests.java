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
    private static final double BBOX_MIN_LON = 113.2;
    private static final double BBOX_MIN_LAT = 23.0;
    private static final double BBOX_MAX_LON = 113.5;
    private static final double BBOX_MAX_LAT = 23.2;
    private static final long BBOX_SEED = 42L;

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

    @Test
    void bboxModeProducesRequestedCount() throws Exception {
        GeoGenerationRequest request = bboxRequest(30);

        List<Map<String, Object>> rows = GeoSyntheticGenerator.generateRows(request);

        Assertions.assertEquals(30, rows.size());
        Assertions.assertTrue(rows.get(0).containsKey("lat"));
        Assertions.assertTrue(rows.get(0).containsKey("lon"));
        assertAllRowsInsideBbox(rows);
    }

    @Test
    void bboxModeSameSeedSameRows() throws Exception {
        GeoGenerationRequest first = bboxRequest(15);
        GeoGenerationRequest second = bboxRequest(15);

        List<Map<String, Object>> firstRows = GeoSyntheticGenerator.generateRows(first);
        List<Map<String, Object>> secondRows = GeoSyntheticGenerator.generateRows(second);

        Assertions.assertEquals(firstRows.size(), secondRows.size());
        for (int i = 0; i < firstRows.size(); i++) {
            Assertions.assertEquals(firstRows.get(i).get("lon"), secondRows.get(i).get("lon"));
            Assertions.assertEquals(firstRows.get(i).get("lat"), secondRows.get(i).get("lat"));
        }
    }

    private static GeoGenerationRequest bboxRequest(int count) {
        GeoGenerationRequest request = new GeoGenerationRequest();
        request.setMode(GeoGenerationMode.BBOX);
        request.setBboxMinLon(BBOX_MIN_LON);
        request.setBboxMinLat(BBOX_MIN_LAT);
        request.setBboxMaxLon(BBOX_MAX_LON);
        request.setBboxMaxLat(BBOX_MAX_LAT);
        request.setCount(count);
        request.setSeed(BBOX_SEED);
        request.setMinDistanceMeters(0d);
        request.setOutputFormat(GeoOutputFormatKind.columns);
        return request;
    }

    private static void assertAllRowsInsideBbox(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            double lon = ((Number) row.get("lon")).doubleValue();
            double lat = ((Number) row.get("lat")).doubleValue();
            Assertions.assertTrue(lon >= BBOX_MIN_LON && lon <= BBOX_MAX_LON,
                    "lon out of bbox: " + lon);
            Assertions.assertTrue(lat >= BBOX_MIN_LAT && lat <= BBOX_MAX_LAT,
                    "lat out of bbox: " + lat);
        }
    }
}
