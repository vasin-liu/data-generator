/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.geo.format.GeoValueFormatter;
import org.gensokyo.data.geo.generate.BoundaryGeometryNormalizer;
import org.gensokyo.data.geo.generate.BoundaryPointGenerator;
import org.gensokyo.data.geo.generate.LineComponentSelector;
import org.gensokyo.data.geo.generate.LineSampleGenerator;
import org.gensokyo.data.geo.io.GeoFeature;
import org.gensokyo.data.geo.io.GeoJsonLoader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates synthetic geospatial rows from a {@link GeoGenerationRequest}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoSyntheticGenerator {

    private static final String PROPERTY_PREFIX = "prop.";

    private GeoSyntheticGenerator() {
    }

    /**
     * Generates formatted row maps for the request.
     *
     * @param request generation configuration
     * @return one map per output row
     * @throws IOException when GeoJSON cannot be read
     */
    public static List<Map<String, Object>> generateRows(GeoGenerationRequest request) throws IOException {
        request.validate();
        List<Point> points;
        Map<String, Object> properties;
        if (request.getMode() == GeoGenerationMode.LINE_SAMPLE && request.isIncludeProperties()) {
            GeoFeature feature = GeoJsonLoader.loadFeature(
                    request.getNetworkPath(),
                    request.getFeatureIndex(),
                    request.isRandomFeature(),
                    request.getSeed());
            LineString line = LineComponentSelector.selectLongestLineString(feature.geometry());
            points = LineSampleGenerator.sample(
                    line, request.getSampleStrategy(), request.getCount(), request.getSpacingMeters());
            properties = feature.properties();
        } else {
            points = generatePointsInternal(request);
            properties = Map.of();
        }
        ensureNoColumnCollisions(request, properties);

        List<Map<String, Object>> rows = new ArrayList<>(points.size());
        for (Point point : points) {
            Map<String, Object> row = new LinkedHashMap<>(
                    GeoValueFormatter.formatPoint(point, request.getOutputFormat(), request.getColumnNames()));
            if (request.isIncludeProperties() && !properties.isEmpty()) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    row.put(PROPERTY_PREFIX + entry.getKey(), entry.getValue());
                }
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Generates sample points without formatting.
     *
     * @param request generation configuration
     * @return sample points
     * @throws IOException when GeoJSON cannot be read
     */
    public static List<Point> generatePoints(GeoGenerationRequest request) throws IOException {
        request.validate();
        return generatePointsInternal(request);
    }

    private static List<Point> generatePointsInternal(GeoGenerationRequest request) throws IOException {
        return switch (request.getMode()) {
            case BOUNDARY_POINTS -> generateBoundaryPoints(request);
            case LINE_SAMPLE -> generateLinePoints(request);
            // Sampling for BBOX/CIRCLE arrives in a follow-on plan; validation is wired in Phase 18-01.
            case BBOX, CIRCLE -> throw new UnsupportedOperationException(
                    request.getMode() + " sampling is not implemented yet");
        };
    }

    private static List<Point> generateBoundaryPoints(GeoGenerationRequest request) throws IOException {
        Geometry geometry = GeoJsonLoader.loadGeometry(request.getBoundaryPath(), request.getFeatureIndex());
        Geometry normalized = BoundaryGeometryNormalizer.normalize(geometry);
        return BoundaryPointGenerator.generate(
                normalized,
                request.getCount(),
                request.getMinDistanceMeters(),
                request.getSeed());
    }

    private static List<Point> generateLinePoints(GeoGenerationRequest request) throws IOException {
        GeoFeature feature = GeoJsonLoader.loadFeature(
                request.getNetworkPath(),
                request.getFeatureIndex(),
                request.isRandomFeature(),
                request.getSeed());
        LineString line = LineComponentSelector.selectLongestLineString(feature.geometry());
        return LineSampleGenerator.sample(
                line,
                request.getSampleStrategy(),
                request.getCount(),
                request.getSpacingMeters());
    }

    private static void ensureNoColumnCollisions(GeoGenerationRequest request, Map<String, Object> properties) {
        if (!request.isIncludeProperties() || properties == null || properties.isEmpty()) {
            return;
        }
        List<String> geometryColumns = GeoValueFormatter.columnNames(request.getOutputFormat(), request.getColumnNames());
        for (String propertyKey : properties.keySet()) {
            String column = PROPERTY_PREFIX + propertyKey;
            if (geometryColumns.contains(column)) {
                throw new IllegalArgumentException("Property key collides with geometry column: " + column);
            }
        }
    }
}
