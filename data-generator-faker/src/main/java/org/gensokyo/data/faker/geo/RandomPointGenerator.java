/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker.geo;

import org.locationtech.jts.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机经纬度生成器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/8 , Version 1.0.0
 */
public class RandomPointGenerator {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public static List<Point> generate(
            Geometry geometry,
            int totalCount,
            double minDistanceMeters,
            long seed,
            int maxRetries
    ) {
        List<Point> points = new ArrayList<>(totalCount);
        Random random = new Random(seed);

        if (geometry instanceof MultiPolygon multiPolygon) {
            // 按面积比例分配每个Polygon应该生成的点数
            List<Integer> pointsPerPolygon = distributePointsByArea(multiPolygon, totalCount);
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
                points.addAll(generateInSingleGeometry(polygon, pointsPerPolygon.get(i), minDistanceMeters, random, maxRetries));
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
            int maxRetries
    ) {
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
            throw new RuntimeException("Exceeded maximum retries while generating points");
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

        // 修正误差（防止少分配）
        while (assignedPoints < totalPoints) {
            distribution.set(0, distribution.get(0) + 1);
            assignedPoints++;
        }

        return distribution;
    }

    private static boolean isFarEnough(Point point, List<Point> existingPoints, double minDistanceMeters) {
        for (Point existing : existingPoints) {
            if (haversineDistance(point.getY(), point.getX(), existing.getY(), existing.getX()) < minDistanceMeters) {
                return false;
            }
        }
        return true;
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // 地球半径（米）
        final double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
