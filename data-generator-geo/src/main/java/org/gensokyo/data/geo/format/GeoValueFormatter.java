/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.format;

import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTWriter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats JTS points into row maps for templates and Calcite sources.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoValueFormatter {

    private static final WKTWriter WKT_WRITER = new WKTWriter();

    private GeoValueFormatter() {
    }

    /**
     * Formats one point as a row map.
     *
     * @param point       sample point (lon/lat as X/Y)
     * @param format      output format
     * @param columnNames optional column renames
     * @return row values keyed by column name
     */
    public static Map<String, Object> formatPoint(
            Point point,
            GeoOutputFormatKind format,
            GeoOutputColumnNames columnNames) {
        GeoOutputColumnNames names = columnNames == null ? new GeoOutputColumnNames() : columnNames;
        return switch (format) {
            case columns -> formatColumns(point, names);
            case geojson -> Map.of(names.getGeometry(), toGeoJsonPoint(point));
            case wkt -> Map.of(names.getGeometry(), WKT_WRITER.write(point));
        };
    }

    /**
     * Returns SQL column names for the given output format.
     *
     * @param format      output format
     * @param columnNames optional renames
     * @return ordered column names
     */
    public static List<String> columnNames(GeoOutputFormatKind format, GeoOutputColumnNames columnNames) {
        GeoOutputColumnNames names = columnNames == null ? new GeoOutputColumnNames() : columnNames;
        return switch (format) {
            case columns -> {
                List<String> cols = new ArrayList<>();
                cols.add(names.getLat());
                cols.add(names.getLon());
                yield cols;
            }
            case geojson, wkt -> List.of(names.getGeometry());
        };
    }

    private static Map<String, Object> formatColumns(Point point, GeoOutputColumnNames names) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(names.getLat(), point.getY());
        row.put(names.getLon(), point.getX());
        Coordinate coordinate = point.getCoordinate();
        if (coordinate != null && !Double.isNaN(coordinate.getZ())) {
            row.put(names.getAlt(), coordinate.getZ());
        }
        return row;
    }

    private static String toGeoJsonPoint(Point point) {
        Coordinate coordinate = point.getCoordinate();
        if (coordinate == null || Double.isNaN(coordinate.getZ())) {
            return "{\"type\":\"Point\",\"coordinates\":[" + point.getX() + "," + point.getY() + "]}";
        }
        return "{\"type\":\"Point\",\"coordinates\":[" + coordinate.x + "," + coordinate.y + "," + coordinate.getZ() + "]}";
    }
}
