/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;
import java.util.Map;

/**
 * Capped synthetic point preview payload for console map honesty messaging (D-08/D-10).
 *
 * @param seed                 effective seed used for generation
 * @param effectiveSampleCount number of points actually returned
 * @param maxCountCap          hard preview cap (500)
 * @param featureCollection    GeoJSON FeatureCollection of Point features
 * @author Gensokyo
 * @since 2026-08-06
 */
public record GeoSyntheticPreviewView(
        long seed,
        int effectiveSampleCount,
        int maxCountCap,
        Map<String, Object> featureCollection) {

    /**
     * Builds a view from generated lon/lat rows.
     *
     * @param seed      effective seed
     * @param maxCap    honesty cap constant
     * @param pointRows generator rows with {@code lon}/{@code lat} columns
     * @return preview view
     */
    public static GeoSyntheticPreviewView fromRows(long seed, int maxCap, List<Map<String, Object>> pointRows) {
        List<Map<String, Object>> features = pointRows.stream()
                .map(GeoSyntheticPreviewView::toPointFeature)
                .toList();
        Map<String, Object> collection = Map.of(
                "type", "FeatureCollection",
                "features", features);
        return new GeoSyntheticPreviewView(seed, features.size(), maxCap, collection);
    }

    private static Map<String, Object> toPointFeature(Map<String, Object> row) {
        Object lon = row.get("lon");
        Object lat = row.get("lat");
        return Map.of(
                "type", "Feature",
                "geometry", Map.of(
                        "type", "Point",
                        "coordinates", List.of(lon, lat)),
                "properties", Map.of());
    }
}
