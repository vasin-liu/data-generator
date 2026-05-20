/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Haversine distance helpers for geodesic length and spacing along lines.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoHaversine {

    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private GeoHaversine() {
    }

    /**
     * Returns great-circle distance in meters between two WGS84 coordinates (x=lon, y=lat).
     *
     * @param from first coordinate
     * @param to   second coordinate
     * @return distance in meters
     */
    public static double distanceMeters(Coordinate from, Coordinate to) {
        return distanceMeters(from.y, from.x, to.y, to.x);
    }

    /**
     * Returns great-circle distance in meters.
     *
     * @param lat1 latitude of first point
     * @param lon1 longitude of first point
     * @param lat2 latitude of second point
     * @param lon2 longitude of second point
     * @return distance in meters
     */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }

    /**
     * Sums haversine segment lengths along a line string.
     *
     * @param lineString line geometry (lon/lat vertices)
     * @return total length in meters
     */
    public static double lineLengthMeters(LineString lineString) {
        if (lineString == null || lineString.getNumPoints() < 2) {
            return 0d;
        }
        double total = 0d;
        Coordinate[] coords = lineString.getCoordinates();
        for (int i = 1; i < coords.length; i++) {
            total += distanceMeters(coords[i - 1], coords[i]);
        }
        return total;
    }
}
