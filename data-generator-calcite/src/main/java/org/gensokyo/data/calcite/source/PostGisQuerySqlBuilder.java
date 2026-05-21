/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.format.GeoOutputColumnNames;
import org.gensokyo.data.model.v2.GeoJsonSourceOutputVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds PostGIS {@code SELECT} statements for {@link PostGisQuerySourceVO}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class PostGisQuerySqlBuilder {

    private static final String IDENTIFIER_PATTERN = "[A-Za-z_][A-Za-z0-9_]*";

    private PostGisQuerySqlBuilder() {
    }

    /**
     * Builds a single-table PostGIS query projecting geometry into the configured output shape.
     *
     * @param source PostGIS source configuration
     * @return SQL text (no trailing semicolon)
     */
    public static String buildSelect(PostGisQuerySourceVO source) {
        if (source.getTable() == null || source.getTable().isBlank()) {
            throw new IllegalArgumentException("POSTGIS source table must not be blank");
        }
        String table = requireIdentifier(source.getTable(), "table");
        String geometryColumn = source.getGeometryColumn() == null || source.getGeometryColumn().isBlank()
                ? "geom"
                : requireIdentifier(source.getGeometryColumn(), "geometryColumn");

        GeoJsonSourceOutputVO output = source.getOutput() == null ? new GeoJsonSourceOutputVO() : source.getOutput();
        GeoOutputFormatKind format = output.getFormat() == null ? GeoOutputFormatKind.columns : output.getFormat();
        GeoOutputColumnNames names = output.getColumnNames() == null ? new GeoOutputColumnNames() : output.getColumnNames();
        boolean includeProperties = output.isIncludeProperties();

        List<String> projections = new ArrayList<>();
        String geomExpr = table + "." + geometryColumn;
        switch (format) {
            case columns -> {
                projections.add("ST_Y(ST_PointOnSurface(" + geomExpr + ")) AS " + names.getLat());
                projections.add("ST_X(ST_PointOnSurface(" + geomExpr + ")) AS " + names.getLon());
            }
            case wkt -> projections.add("ST_AsText(" + geomExpr + ") AS " + names.getGeometry());
            case geojson -> projections.add("ST_AsGeoJSON(" + geomExpr + ") AS " + names.getGeometry());
        }

        if (source.getAttributes() != null) {
            for (String attribute : source.getAttributes()) {
                if (attribute == null || attribute.isBlank()) {
                    continue;
                }
                String column = requireIdentifier(attribute, "attribute");
                String alias = includeProperties ? quoteAlias("prop." + column) : column;
                projections.add(table + "." + column + " AS " + alias);
            }
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(String.join(", ", projections));
        sql.append(" FROM ").append(table);
        if (source.getWhere() != null && !source.getWhere().isBlank()) {
            validateWhereClause(source.getWhere());
            sql.append(" WHERE ").append(source.getWhere().trim());
        }
        return sql.toString();
    }

    private static String requireIdentifier(String value, String label) {
        String trimmed = value.trim();
        if (!trimmed.matches(IDENTIFIER_PATTERN)) {
            throw new IllegalArgumentException("Invalid POSTGIS " + label + " identifier: " + value);
        }
        return trimmed;
    }

    private static String quoteAlias(String alias) {
        return "\"" + alias.replace("\"", "\"\"") + "\"";
    }

    private static void validateWhereClause(String where) {
        String normalized = where.toLowerCase(Locale.ROOT);
        if (normalized.contains(";") || normalized.contains("--")) {
            throw new IllegalArgumentException("POSTGIS where clause must not contain ';' or '--'");
        }
    }
}
