/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.model.v2.GeoJsonSourceVO;

/**
 * Resolves Template V2 {@link GeoJsonSourceVO} locations for runtime loading (GEO-10/D-01..D-03).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
public final class GeoJsonLocationMapper {

    private GeoJsonLocationMapper() {
    }

    /**
     * Resolves the effective GeoJSON location for a geojson source.
     *
     * @param sourceName logical Template V2 source name (included in validation errors)
     * @param source     geojson source configuration
     * @return classpath, filesystem, or {@code asset:{uuid}} location
     * @throws IllegalArgumentException when path and asset-id are both set, or neither yields a location
     */
    public static String resolveLocation(String sourceName, GeoJsonSourceVO source) {
        String trimmedPath = blankToNull(source.getPath());
        String trimmedAssetId = blankToNull(source.getAssetId());
        if (trimmedPath != null && trimmedAssetId != null) {
            throw new IllegalArgumentException(
                    "GEOJSON source [" + sourceName + "]: path and assetId are both set; use one binding only");
        }
        if (trimmedAssetId != null) {
            return GeoSyntheticRequestMapper.toAssetLocation(trimmedAssetId);
        }
        if (trimmedPath != null) {
            return trimmedPath;
        }
        throw new IllegalArgumentException("GEOJSON source path must not be blank for source [" + sourceName + "]");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
