/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.generate;

import org.gensokyo.data.geo.GeoHaversine;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates random points uniformly inside a circle using area-uniform polar sampling.
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
public final class CirclePointGenerator {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final int DEFAULT_MAX_RETRIES = 10_000;
    // Duplicated from GeoHaversine (EARTH_RADIUS_METERS is private) for local meter→degree projection.
    private static final double EARTH_RADIUS_METERS = 6_371_000d;
    private static final double TWO_PI = 2d * Math.PI;

    private CirclePointGenerator() {
    }

    /**
     * Generates random points inside a Haversine circle around the center.
     *
     * @param centerLon           circle center longitude (WGS84)
     * @param centerLat           circle center latitude (WGS84)
     * @param radiusMeters        circle radius in meters
     * @param count               number of points
     * @param minDistanceMeters   minimum spacing between points (0 to disable)
     * @param seed                random seed
     * @return generated points (x=lon, y=lat)
     */
    public static List<Point> generate(
            double centerLon,
            double centerLat,
            double radiusMeters,
            int count,
            double minDistanceMeters,
            long seed) {
        return generate(centerLon, centerLat, radiusMeters, count, minDistanceMeters, seed, DEFAULT_MAX_RETRIES);
    }

    /**
     * Generates random points inside a Haversine circle around the center.
     *
     * @param centerLon           circle center longitude (WGS84)
     * @param centerLat           circle center latitude (WGS84)
     * @param radiusMeters        circle radius in meters
     * @param count               number of points
     * @param minDistanceMeters   minimum spacing between points (0 to disable)
     * @param seed                random seed
     * @param maxRetries          sampling retries before failure
     * @return generated points (x=lon, y=lat)
     */
    public static List<Point> generate(
            double centerLon,
            double centerLat,
            double radiusMeters,
            int count,
            double minDistanceMeters,
            long seed,
            int maxRetries) {
        List<Point> points = new ArrayList<>(count);
        Random random = new Random(seed);
        double cosCenterLat = Math.cos(Math.toRadians(centerLat));

        int retries = 0;
        while (points.size() < count && retries < maxRetries) {
            for (int i = 0; i < 100; i++) {
                // Area-uniform polar sampling: r = R * sqrt(u), theta = 2*pi*v (D-04).
                double u = random.nextDouble();
                double v = random.nextDouble();
                double rMeters = radiusMeters * Math.sqrt(u);
                double theta = TWO_PI * v;

                double eastMeters = rMeters * Math.cos(theta);
                double northMeters = rMeters * Math.sin(theta);

                // Local east/north meters → degree offsets at center latitude.
                double deltaLatDegrees = Math.toDegrees(northMeters / EARTH_RADIUS_METERS);
                double deltaLonDegrees = Math.toDegrees(eastMeters / (EARTH_RADIUS_METERS * cosCenterLat));
                double candidateLat = centerLat + deltaLatDegrees;
                double candidateLon = centerLon + deltaLonDegrees;

                // Haversine gate: accept only points within the configured radius (D-04).
                if (GeoHaversine.distanceMeters(centerLat, centerLon, candidateLat, candidateLon) > radiusMeters) {
                    continue;
                }

                Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(candidateLon, candidateLat));
                if (isFarEnough(point, points, minDistanceMeters)) {
                    points.add(point);
                    if (points.size() >= count) {
                        break;
                    }
                }
            }
            retries++;
        }

        if (retries >= maxRetries) {
            throw new RuntimeException("Exceeded maximum retries while generating circle points");
        }

        return points;
    }

    private static boolean isFarEnough(Point point, List<Point> existingPoints, double minDistanceMeters) {
        if (minDistanceMeters <= 0) {
            return true;
        }
        for (Point existing : existingPoints) {
            if (GeoHaversine.distanceMeters(point.getY(), point.getX(), existing.getY(), existing.getX())
                    < minDistanceMeters) {
                return false;
            }
        }
        return true;
    }
}
