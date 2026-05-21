/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.geo.io.GeoJsonLoader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * GeoJSON parse and JTS topological predicates for Template V2 geo SQL functions.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class GeoJsonPredicates {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private GeoJsonPredicates() {
    }

    /**
     * Parses a GeoJSON geometry or Feature JSON string.
     *
     * @param geoJson geometry or feature JSON
     * @return parsed geometry
     */
    public static Geometry parse(String geoJson) {
        return GeoJsonLoader.parseGeometryJson(geoJson);
    }

    /**
     * Whether two GeoJSON geometries intersect.
     *
     * @param geoJson1 first geometry JSON
     * @param geoJson2 second geometry JSON
     * @return {@code true} when geometries intersect
     */
    public static boolean intersects(String geoJson1, String geoJson2) {
        return parse(geoJson1).intersects(parse(geoJson2));
    }

    /**
     * Whether the first GeoJSON geometry contains the second.
     *
     * @param outerGeoJson container geometry JSON
     * @param innerGeoJson geometry that may lie inside {@code outerGeoJson}
     * @return {@code true} when outer contains inner
     */
    public static boolean contains(String outerGeoJson, String innerGeoJson) {
        return parse(outerGeoJson).contains(parse(innerGeoJson));
    }

    /**
     * Whether a WGS84 point lies inside a GeoJSON region geometry.
     *
     * @param lat        latitude
     * @param lon        longitude
     * @param areaGeoJson region geometry JSON
     * @return {@code true} when the point is contained
     */
    public static boolean pointInGeoJson(double lat, double lon, String areaGeoJson) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
        return parse(areaGeoJson).contains(point);
    }
}
