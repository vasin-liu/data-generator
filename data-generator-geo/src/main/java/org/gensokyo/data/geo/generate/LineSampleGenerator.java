/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.generate;

import org.gensokyo.data.geo.GeoHaversine;
import org.gensokyo.data.geo.GeoSampleStrategyKind;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Samples points along a line using count-based or fixed-spacing strategies.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class LineSampleGenerator {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private LineSampleGenerator() {
    }

    /**
     * Samples points on a line string.
     *
     * @param lineString     target line
     * @param strategy       BY_COUNT or BY_SPACING_METERS
     * @param count          used when strategy is BY_COUNT
     * @param spacingMeters  used when strategy is BY_SPACING_METERS
     * @return sample points along the line
     */
    public static List<Point> sample(
            LineString lineString,
            GeoSampleStrategyKind strategy,
            int count,
            double spacingMeters) {
        if (lineString == null || lineString.getNumPoints() < 2) {
            throw new IllegalArgumentException("LineString must contain at least two coordinates");
        }
        double totalLength = GeoHaversine.lineLengthMeters(lineString);
        if (totalLength <= 0d) {
            throw new IllegalArgumentException("LineString length must be positive");
        }

        return switch (strategy) {
            case BY_COUNT -> sampleByCount(lineString, count, totalLength);
            case BY_SPACING_METERS -> sampleBySpacing(lineString, spacingMeters, totalLength);
        };
    }

    private static List<Point> sampleByCount(LineString lineString, int count, double totalLength) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive for BY_COUNT");
        }
        if (count == 1) {
            return List.of(pointAtDistance(lineString, 0d));
        }
        List<Point> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double targetDistance = totalLength * i / (count - 1d);
            points.add(pointAtDistance(lineString, targetDistance));
        }
        return points;
    }

    private static List<Point> sampleBySpacing(LineString lineString, double spacingMeters, double totalLength) {
        if (spacingMeters <= 0d) {
            throw new IllegalArgumentException("spacingMeters must be positive for BY_SPACING_METERS");
        }
        int pointCount = (int) Math.floor(totalLength / spacingMeters) + 1;
        List<Point> points = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            double targetDistance = Math.min(totalLength, i * spacingMeters);
            points.add(pointAtDistance(lineString, targetDistance));
        }
        return points;
    }

    private static Point pointAtDistance(LineString lineString, double targetDistanceMeters) {
        Coordinate[] coordinates = lineString.getCoordinates();
        double walked = 0d;
        for (int i = 1; i < coordinates.length; i++) {
            Coordinate from = coordinates[i - 1];
            Coordinate to = coordinates[i];
            double segmentLength = GeoHaversine.distanceMeters(from, to);
            if (walked + segmentLength >= targetDistanceMeters || i == coordinates.length - 1) {
                if (segmentLength <= 0d) {
                    return GEOMETRY_FACTORY.createPoint(to);
                }
                double ratio = Math.min(1d, (targetDistanceMeters - walked) / segmentLength);
                double lon = from.x + (to.x - from.x) * ratio;
                double lat = from.y + (to.y - from.y) * ratio;
                double z = Double.isNaN(from.z) && Double.isNaN(to.z)
                        ? Double.NaN
                        : from.z + (to.z - from.z) * ratio;
                return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat, z));
            }
            walked += segmentLength;
        }
        return GEOMETRY_FACTORY.createPoint(coordinates[coordinates.length - 1]);
    }
}
