/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.generate;

import org.gensokyo.data.geo.GeoHaversine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link CirclePointGenerator}.
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
class CirclePointGeneratorTests {

    private static final double CENTER_LON = 113.3;
    private static final double CENTER_LAT = 23.1;
    private static final double RADIUS_METERS = 500d;
    private static final long SEED = 42L;

    @Test
    void allPointsWithinRadius() {
        List<Point> points = CirclePointGenerator.generate(
                CENTER_LON, CENTER_LAT, RADIUS_METERS, 50, 0d, SEED);

        Assertions.assertEquals(50, points.size());
        for (Point point : points) {
            double distance = GeoHaversine.distanceMeters(
                    CENTER_LAT, CENTER_LON, point.getY(), point.getX());
            Assertions.assertTrue(distance <= RADIUS_METERS,
                    "Point outside circle: " + distance + " m");
        }
    }

    @Test
    void sameSeed_producesIdenticalPoints() {
        List<Point> first = CirclePointGenerator.generate(
                CENTER_LON, CENTER_LAT, RADIUS_METERS, 10, 0d, SEED);
        List<Point> second = CirclePointGenerator.generate(
                CENTER_LON, CENTER_LAT, RADIUS_METERS, 10, 0d, SEED);

        Assertions.assertEquals(toCoordinates(first), toCoordinates(second));
    }

    @Test
    void requestedCount_matches() {
        List<Point> points = CirclePointGenerator.generate(
                CENTER_LON, CENTER_LAT, RADIUS_METERS, 100, 0d, SEED);

        Assertions.assertEquals(100, points.size());
    }

    @Test
    void minDistanceTooLarge_throwsRuntimeExceptionOnRetryExhaustion() {
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () ->
                CirclePointGenerator.generate(
                        CENTER_LON, CENTER_LAT, RADIUS_METERS,
                        2, 400d, SEED, 5));

        Assertions.assertTrue(ex.getMessage().contains("retries"),
                "Expected retries exhaustion message, got: " + ex.getMessage());
    }

    @Test
    void minDistancePositive_respectsSpacing() {
        double minDistance = 200d;
        List<Point> points = CirclePointGenerator.generate(
                CENTER_LON, CENTER_LAT, RADIUS_METERS, 5, minDistance, SEED);

        Assertions.assertEquals(5, points.size());
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                Point a = points.get(i);
                Point b = points.get(j);
                double distance = GeoHaversine.distanceMeters(
                        a.getY(), a.getX(), b.getY(), b.getX());
                Assertions.assertTrue(distance >= minDistance,
                        "Points too close: " + distance + " m");
            }
        }
    }

    private static List<String> toCoordinates(List<Point> points) {
        return points.stream()
                .map(p -> p.getX() + "," + p.getY())
                .collect(Collectors.toList());
    }
}
