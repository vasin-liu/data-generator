/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * WKT parse and JTS topological predicates for Template V2 geo SQL functions.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class GeoWktPredicates {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final WKTReader WKT_READER = new WKTReader(GEOMETRY_FACTORY);

    private GeoWktPredicates() {
    }

    /**
     * Parses a WKT geometry string (WGS84, x=longitude, y=latitude).
     *
     * @param wkt Well-Known Text
     * @return parsed geometry
     * @throws IllegalArgumentException when WKT is blank or invalid
     */
    public static Geometry parse(String wkt) {
        if (wkt == null || wkt.isBlank()) {
            throw new IllegalArgumentException("WKT must not be blank");
        }
        try {
            return WKT_READER.read(wkt.strip());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid WKT: " + wkt, e);
        }
    }

    /**
     * Whether two WKT geometries intersect.
     *
     * @param wkt1 first geometry WKT
     * @param wkt2 second geometry WKT
     * @return {@code true} when geometries intersect
     */
    public static boolean intersects(String wkt1, String wkt2) {
        return parse(wkt1).intersects(parse(wkt2));
    }

    /**
     * Whether the first WKT geometry fully contains the second.
     *
     * @param outerWkt container geometry WKT
     * @param innerWkt geometry that may lie inside {@code outerWkt}
     * @return {@code true} when {@code outerWkt} contains {@code innerWkt}
     */
    public static boolean contains(String outerWkt, String innerWkt) {
        return parse(outerWkt).contains(parse(innerWkt));
    }

    /**
     * Whether a WGS84 point lies inside (or on the boundary of) a WKT geometry.
     *
     * @param lat    latitude
     * @param lon    longitude
     * @param areaWkt region geometry WKT
     * @return {@code true} when the point is contained
     */
    public static boolean pointInWkt(double lat, double lon, String areaWkt) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
        return parse(areaWkt).contains(point);
    }
}
