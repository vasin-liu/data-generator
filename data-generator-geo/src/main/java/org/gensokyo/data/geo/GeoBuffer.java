/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.geo.format.GeoJsonGeometryEncoder;
import org.gensokyo.data.geo.io.GeoJsonLoader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;

/**
 * Approximate geodesic buffer for WGS84 geometries (engineering accuracy, not survey-grade).
 * <p>
 * Converts buffer distance from meters to degrees at the geometry centroid latitude, then applies
 * JTS {@link Geometry#buffer(double)}. Suitable for small radii (roughly sub-10 km) at mid-latitudes.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class GeoBuffer {

    /** Mean meters per degree of latitude (WGS84 approximation). */
    private static final double METERS_PER_DEGREE_LAT = 111_320d;

    private static final WKTWriter WKT_WRITER = new WKTWriter();

    private GeoBuffer() {
    }

    /**
     * Buffers a WKT geometry outward by approximately {@code distanceMeters}.
     *
     * @param wkt             input geometry WKT
     * @param distanceMeters  buffer distance in meters (must be &gt;= 0)
     * @return buffered geometry as WKT
     */
    public static String bufferWkt(String wkt, double distanceMeters) {
        return WKT_WRITER.write(buffer(GeoWktPredicates.parse(wkt), distanceMeters));
    }

    /**
     * Buffers a GeoJSON geometry outward by approximately {@code distanceMeters}.
     *
     * @param geoJson          geometry or Feature JSON
     * @param distanceMeters   buffer distance in meters (must be &gt;= 0)
     * @return buffered geometry as GeoJSON geometry object text
     */
    public static String bufferGeoJson(String geoJson, double distanceMeters) {
        return GeoJsonGeometryEncoder.encode(buffer(GeoJsonLoader.parseGeometryJson(geoJson), distanceMeters));
    }

    /**
     * Buffers a JTS geometry in place using a degree distance derived from meters.
     *
     * @param geometry         input geometry (lon/lat)
     * @param distanceMeters   buffer distance in meters
     * @return buffered geometry
     */
    public static Geometry buffer(Geometry geometry, double distanceMeters) {
        if (geometry == null || geometry.isEmpty()) {
            throw new IllegalArgumentException("geometry must not be null or empty");
        }
        if (distanceMeters < 0) {
            throw new IllegalArgumentException("distanceMeters must be >= 0");
        }
        if (distanceMeters == 0) {
            return geometry.copy();
        }
        // Scale meters to degrees at centroid latitude for JTS buffer (same CRS as geo rows).
        double centroidLat = geometry.getCentroid().getY();
        double metersPerDegree = METERS_PER_DEGREE_LAT * Math.max(0.01, Math.cos(Math.toRadians(centroidLat)));
        double bufferDegrees = distanceMeters / metersPerDegree;
        return geometry.buffer(bufferDegrees);
    }
}
