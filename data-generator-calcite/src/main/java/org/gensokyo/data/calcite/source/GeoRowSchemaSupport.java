/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.format.GeoOutputColumnNames;
import org.gensokyo.data.geo.format.GeoValueFormatter;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link RowSchema} for GEO iterator and GEOJSON file sources from formatted row maps.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoRowSchemaSupport {

    private GeoRowSchemaSupport() {
    }

    /**
     * Infers column definitions from the first generated row, or falls back to formatter column names.
     *
     * @param format      output format (columns vs single geometry column)
     * @param columnNames optional renames
     * @param generated   formatted rows (may be empty)
     * @return schema for Calcite exposure
     */
    public static RowSchema schemaForGeoRows(
            GeoOutputFormatKind format,
            GeoOutputColumnNames columnNames,
            List<Map<String, Object>> generated) {
        List<ColumnDef> columns = new ArrayList<>();
        if (generated.isEmpty()) {
            for (String column : GeoValueFormatter.columnNames(format, columnNames)) {
                String sqlType = format == GeoOutputFormatKind.columns ? "DOUBLE" : "VARCHAR";
                columns.add(new ColumnDef(column, sqlType, false));
            }
        } else {
            for (String key : generated.get(0).keySet()) {
                Object value = generated.get(0).get(key);
                boolean nullable = key.startsWith("prop.");
                String sqlType = sqlTypeForGeoColumn(format, columnNames, key, value);
                columns.add(new ColumnDef(key, sqlType, nullable));
            }
        }
        RowSchema schema = new RowSchema();
        schema.setColumns(columns);
        return schema;
    }

    private static String sqlTypeForGeoColumn(
            GeoOutputFormatKind format,
            GeoOutputColumnNames columnNames,
            String key,
            Object value) {
        if (format == GeoOutputFormatKind.columns && columnNames != null && key.equals(columnNames.getAlt())) {
            return "DOUBLE";
        }
        if (value instanceof Number) {
            return "DOUBLE";
        }
        return "VARCHAR";
    }
}
