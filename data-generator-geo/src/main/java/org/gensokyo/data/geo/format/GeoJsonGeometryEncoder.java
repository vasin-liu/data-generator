/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.format;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Minimal RFC 7946-style GeoJSON geometry encoder for JTS {@link Geometry} instances (WGS84 assumed).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoJsonGeometryEncoder {

    private GeoJsonGeometryEncoder() {
    }

    /**
     * Encodes a non-empty geometry as a GeoJSON geometry JSON object string.
     *
     * @param geometry JTS geometry to encode
     * @return GeoJSON geometry object text (no Feature wrapper)
     */
    public static String encode(Geometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            throw new IllegalArgumentException("geometry must not be null or empty");
        }
        return encodeNonEmpty(geometry);
    }

    private static String encodeNonEmpty(Geometry geometry) {
        return switch (geometry.getGeometryType()) {
            case "Point" -> encodePoint((Point) geometry);
            case "MultiPoint" -> encodeMultiPoint((MultiPoint) geometry);
            case "LineString" -> encodeLineString((LineString) geometry);
            case "MultiLineString" -> encodeMultiLineString((MultiLineString) geometry);
            case "Polygon" -> encodePolygon((Polygon) geometry);
            case "MultiPolygon" -> encodeMultiPolygon((MultiPolygon) geometry);
            case "GeometryCollection" -> encodeGeometryCollection((GeometryCollection) geometry);
            default -> throw new IllegalArgumentException("Unsupported geometry type for GeoJSON encoding: "
                    + geometry.getGeometryType());
        };
    }

    private static String encodePoint(Point point) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Point\",\"coordinates\":");
        appendCoordinate(sb, point.getCoordinate());
        sb.append('}');
        return sb.toString();
    }

    private static String encodeMultiPoint(MultiPoint multiPoint) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"MultiPoint\",\"coordinates\":[");
        for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendCoordinate(sb, multiPoint.getGeometryN(i).getCoordinate());
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String encodeLineString(LineString lineString) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"LineString\",\"coordinates\":");
        appendCoordinateSequence(sb, lineString);
        sb.append('}');
        return sb.toString();
    }

    private static String encodeMultiLineString(MultiLineString multiLineString) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"MultiLineString\",\"coordinates\":[");
        for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendCoordinateSequence(sb, (LineString) multiLineString.getGeometryN(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String encodePolygon(Polygon polygon) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Polygon\",\"coordinates\":[");
        appendLinearRingCoordinates(sb, polygon.getExteriorRing());
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            sb.append(',');
            appendLinearRingCoordinates(sb, polygon.getInteriorRingN(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String encodeMultiPolygon(MultiPolygon multiPolygon) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"MultiPolygon\",\"coordinates\":[");
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
            sb.append('[');
            appendLinearRingCoordinates(sb, polygon.getExteriorRing());
            for (int h = 0; h < polygon.getNumInteriorRing(); h++) {
                sb.append(',');
                appendLinearRingCoordinates(sb, polygon.getInteriorRingN(h));
            }
            sb.append(']');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String encodeGeometryCollection(GeometryCollection collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"GeometryCollection\",\"geometries\":[");
        for (int i = 0; i < collection.getNumGeometries(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Geometry child = collection.getGeometryN(i);
            sb.append(encodeNonEmpty(child));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static void appendCoordinateSequence(StringBuilder sb, LineString lineString) {
        sb.append('[');
        for (int i = 0; i < lineString.getNumPoints(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendCoordinate(sb, lineString.getCoordinateN(i));
        }
        sb.append(']');
    }

    private static void appendLinearRingCoordinates(StringBuilder sb, LinearRing ring) {
        sb.append('[');
        int n = ring.getNumPoints();
        // GeoJSON rings are closed; emit coordinates including duplicated closing vertex when JTS provides it.
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendCoordinate(sb, ring.getCoordinateN(i));
        }
        sb.append(']');
    }

    private static void appendCoordinate(StringBuilder sb, Coordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("Coordinate must not be null");
        }
        sb.append('[');
        sb.append(coordinate.x);
        sb.append(',');
        sb.append(coordinate.y);
        if (!Double.isNaN(coordinate.getZ())) {
            sb.append(',');
            sb.append(coordinate.getZ());
        }
        sb.append(']');
    }
}
