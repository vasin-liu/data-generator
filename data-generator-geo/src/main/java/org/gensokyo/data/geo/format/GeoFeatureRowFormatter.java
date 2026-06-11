/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.format;

import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.io.GeoFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTWriter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds flat row maps from {@link GeoFeature} payloads for Calcite sources.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoFeatureRowFormatter {

    private static final String PROPERTY_PREFIX = "prop.";
    private static final WKTWriter WKT_WRITER = new WKTWriter();

    private GeoFeatureRowFormatter() {
    }

    /**
     * Formats one GeoJSON feature geometry into template row columns.
     *
     * @param feature           geometry plus optional GeoJSON properties
     * @param format            columns (representative interior point), geojson, or wkt
     * @param columnNames       optional column renames (defaults applied when {@code null})
     * @param includeProperties when {@code true}, merge {@code prop.&lt;key&gt;} entries
     * @return row values keyed by column name
     */
    public static Map<String, Object> format(
            GeoFeature feature,
            GeoOutputFormatKind format,
            GeoOutputColumnNames columnNames,
            boolean includeProperties) {
        GeoOutputFormatKind resolvedFormat = format == null ? GeoOutputFormatKind.columns : format;
        GeoOutputColumnNames names = columnNames == null ? new GeoOutputColumnNames() : columnNames;
        Geometry geometry = feature.geometry();
        if (geometry == null || geometry.isEmpty()) {
            throw new IllegalArgumentException("GeoFeature geometry must not be null or empty");
        }

        Map<String, Object> row = new LinkedHashMap<>();
        switch (resolvedFormat) {
            case columns -> {
                // Representative location for arbitrary geometries (polygon centroid/interior heuristic via JTS).
                Point representative = geometry.getInteriorPoint();
                row.putAll(GeoValueFormatter.formatPoint(representative, GeoOutputFormatKind.columns, names));
            }
            case wkt -> row.put(names.getGeometry(), WKT_WRITER.write(geometry));
            case geojson -> row.put(names.getGeometry(), GeoJsonGeometryEncoder.encode(geometry));
        }

        if (includeProperties && feature.properties() != null && !feature.properties().isEmpty()) {
            ensureNoColumnCollisions(resolvedFormat, names, feature.properties());
            for (Map.Entry<String, Object> entry : feature.properties().entrySet()) {
                row.put(PROPERTY_PREFIX + entry.getKey(), entry.getValue());
            }
        }
        return row;
    }

    private static void ensureNoColumnCollisions(
            GeoOutputFormatKind format,
            GeoOutputColumnNames names,
            Map<String, Object> properties) {
        List<String> geometryColumns = GeoValueFormatter.columnNames(format, names);
        for (String propertyKey : properties.keySet()) {
            String column = PROPERTY_PREFIX + propertyKey;
            if (geometryColumns.contains(column)) {
                throw new IllegalArgumentException("Property key collides with geometry column: " + column);
            }
        }
    }
}
