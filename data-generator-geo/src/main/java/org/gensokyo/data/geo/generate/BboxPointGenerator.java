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
 * Generates random points uniformly inside a WGS84 bounding box.
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
public final class BboxPointGenerator {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final int DEFAULT_MAX_RETRIES = 10_000;

    private BboxPointGenerator() {
    }

    /**
     * Generates random points inside the given bounding box.
     *
     * @param minLon              western bound (inclusive)
     * @param minLat              southern bound (inclusive)
     * @param maxLon              eastern bound (inclusive)
     * @param maxLat              northern bound (inclusive)
     * @param count               number of points
     * @param minDistanceMeters   minimum spacing between points (0 to disable)
     * @param seed                random seed
     * @return generated points (x=lon, y=lat)
     */
    public static List<Point> generate(
            double minLon,
            double minLat,
            double maxLon,
            double maxLat,
            int count,
            double minDistanceMeters,
            long seed) {
        return generate(minLon, minLat, maxLon, maxLat, count, minDistanceMeters, seed, DEFAULT_MAX_RETRIES);
    }

    /**
     * Generates random points inside the given bounding box.
     *
     * @param minLon              western bound (inclusive)
     * @param minLat              southern bound (inclusive)
     * @param maxLon              eastern bound (inclusive)
     * @param maxLat              northern bound (inclusive)
     * @param count               number of points
     * @param minDistanceMeters   minimum spacing between points (0 to disable)
     * @param seed                random seed
     * @param maxRetries          sampling retries before failure
     * @return generated points (x=lon, y=lat)
     */
    public static List<Point> generate(
            double minLon,
            double minLat,
            double maxLon,
            double maxLat,
            int count,
            double minDistanceMeters,
            long seed,
            int maxRetries) {
        List<Point> points = new ArrayList<>(count);
        Random random = new Random(seed);
        double lonWidth = maxLon - minLon;
        double latHeight = maxLat - minLat;

        int retries = 0;
        while (points.size() < count && retries < maxRetries) {
            for (int i = 0; i < 100; i++) {
                double lon = minLon + random.nextDouble() * lonWidth;
                double lat = minLat + random.nextDouble() * latHeight;
                Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));

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
            throw new RuntimeException("Exceeded maximum retries while generating bbox points");
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
