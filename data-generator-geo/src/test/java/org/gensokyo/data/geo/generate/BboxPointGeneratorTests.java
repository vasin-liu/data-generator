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
 * Unit tests for {@link BboxPointGenerator}.
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
class BboxPointGeneratorTests {

    private static final double MIN_LON = 113.2;
    private static final double MIN_LAT = 23.0;
    private static final double MAX_LON = 113.5;
    private static final double MAX_LAT = 23.2;
    private static final long SEED = 42L;

    @Test
    void inDomain_allPointsInsideBbox() {
        List<Point> points = BboxPointGenerator.generate(
                MIN_LON, MIN_LAT, MAX_LON, MAX_LAT, 50, 0d, SEED);

        Assertions.assertEquals(50, points.size());
        for (Point point : points) {
            Assertions.assertTrue(point.getX() >= MIN_LON && point.getX() <= MAX_LON,
                    "lon out of bbox: " + point.getX());
            Assertions.assertTrue(point.getY() >= MIN_LAT && point.getY() <= MAX_LAT,
                    "lat out of bbox: " + point.getY());
        }
    }

    @Test
    void sameSeed_producesIdenticalPoints() {
        List<Point> first = BboxPointGenerator.generate(
                MIN_LON, MIN_LAT, MAX_LON, MAX_LAT, 10, 0d, SEED);
        List<Point> second = BboxPointGenerator.generate(
                MIN_LON, MIN_LAT, MAX_LON, MAX_LAT, 10, 0d, SEED);

        Assertions.assertEquals(toCoordinates(first), toCoordinates(second));
    }

    @Test
    void minDistanceZero_returnsRequestedCount() {
        List<Point> points = BboxPointGenerator.generate(
                MIN_LON, MIN_LAT, MAX_LON, MAX_LAT, 100, 0d, SEED);

        Assertions.assertEquals(100, points.size());
    }

    @Test
    void minDistanceTooLarge_throwsRuntimeExceptionOnRetryExhaustion() {
        // Tiny bbox (~11 m wide) with 500 m spacing forces retry exhaustion quickly.
        double tinyMinLon = 113.25;
        double tinyMaxLon = 113.2501;
        double tinyMinLat = 23.1;
        double tinyMaxLat = 23.1001;

        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () ->
                BboxPointGenerator.generate(
                        tinyMinLon, tinyMinLat, tinyMaxLon, tinyMaxLat,
                        2, 500d, SEED, 5));

        Assertions.assertTrue(ex.getMessage().contains("retries"),
                "Expected retries exhaustion message, got: " + ex.getMessage());
    }

    @Test
    void minDistancePositive_respectsSpacing() {
        double minDistance = 1_000d;
        List<Point> points = BboxPointGenerator.generate(
                MIN_LON, MIN_LAT, MAX_LON, MAX_LAT, 5, minDistance, SEED);

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
