/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

import org.gensokyo.data.calcite.TemplateV2SqlFunctionContext;
import org.gensokyo.data.geo.GeoHaversine;

import java.util.Objects;

/**
 * Built-in geospatial helpers for Template V2 SQL transforms (WGS84 lat/lon columns).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class TemplateV2GeoSqlFunctions {

    private TemplateV2GeoSqlFunctions() {
    }

    /**
     * Haversine distance in meters between two WGS84 points.
     *
     * @param context SQL function arguments: lat1, lon1, lat2, lon2
     * @return distance in meters, or {@code null} when any argument is null
     */
    public static Double distanceMeters(TemplateV2SqlFunctionContext context) {
        if (context.arguments().stream().anyMatch(Objects::isNull)) {
            return null;
        }
        // Arguments follow geo row convention: latitude then longitude.
        double lat1 = context.decimalArgument(0).doubleValue();
        double lon1 = context.decimalArgument(1).doubleValue();
        double lat2 = context.decimalArgument(2).doubleValue();
        double lon2 = context.decimalArgument(3).doubleValue();
        return GeoHaversine.distanceMeters(lat1, lon1, lat2, lon2);
    }
}
