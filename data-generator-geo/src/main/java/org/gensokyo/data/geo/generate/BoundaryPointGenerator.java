/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.generate;

import org.gensokyo.data.geo.GeoHaversine;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates random points inside polygonal boundary geometries.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class BoundaryPointGenerator {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final int DEFAULT_MAX_RETRIES = 10_000;

    private BoundaryPointGenerator() {
    }

    /**
     * Generates random points inside the given geometry.
     *
     * @param geometry            normalized boundary geometry
     * @param totalCount          number of points
     * @param minDistanceMeters   minimum spacing between points (0 to disable)
     * @param seed                random seed
     * @return generated points
     */
    public static List<Point> generate(
            Geometry geometry,
            int totalCount,
            double minDistanceMeters,
            long seed) {
        return generate(geometry, totalCount, minDistanceMeters, seed, DEFAULT_MAX_RETRIES);
    }

    /**
     * Generates random points inside the given geometry.
     *
     * @param geometry            normalized boundary geometry
     * @param totalCount          number of points
     * @param minDistanceMeters   minimum spacing between points (0 to disable)
     * @param seed                random seed
     * @param maxRetries          envelope sampling retries
     * @return generated points
     */
    public static List<Point> generate(
            Geometry geometry,
            int totalCount,
            double minDistanceMeters,
            long seed,
            int maxRetries) {
        List<Point> points = new ArrayList<>(totalCount);
        Random random = new Random(seed);

        if (geometry instanceof MultiPolygon multiPolygon) {
            List<Integer> pointsPerPolygon = distributePointsByArea(multiPolygon, totalCount);
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
                points.addAll(generateInSingleGeometry(
                        polygon, pointsPerPolygon.get(i), minDistanceMeters, random, maxRetries));
            }
        } else {
            points.addAll(generateInSingleGeometry(geometry, totalCount, minDistanceMeters, random, maxRetries));
        }

        return points;
    }

    private static List<Point> generateInSingleGeometry(
            Geometry geometry,
            int count,
            double minDistanceMeters,
            Random random,
            int maxRetries) {
        List<Point> points = new ArrayList<>(count);
        Envelope envelope = geometry.getEnvelopeInternal();

        int retries = 0;
        while (points.size() < count && retries < maxRetries) {
            for (int i = 0; i < 100; i++) {
                double x = envelope.getMinX() + random.nextDouble() * envelope.getWidth();
                double y = envelope.getMinY() + random.nextDouble() * envelope.getHeight();
                Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));

                if (geometry.contains(point) && isFarEnough(point, points, minDistanceMeters)) {
                    points.add(point);
                    if (points.size() >= count) {
                        break;
                    }
                }
            }
            retries++;
        }

        if (retries >= maxRetries) {
            throw new RuntimeException("Exceeded maximum retries while generating boundary points");
        }

        return points;
    }

    private static List<Integer> distributePointsByArea(MultiPolygon multiPolygon, int totalPoints) {
        double totalArea = multiPolygon.getArea();
        List<Integer> distribution = new ArrayList<>();
        int assignedPoints = 0;

        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
            int points = (int) Math.round(totalPoints * (polygon.getArea() / totalArea));
            distribution.add(points);
            assignedPoints += points;
        }

        while (assignedPoints < totalPoints) {
            distribution.set(0, distribution.get(0) + 1);
            assignedPoints++;
        }

        return distribution;
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
