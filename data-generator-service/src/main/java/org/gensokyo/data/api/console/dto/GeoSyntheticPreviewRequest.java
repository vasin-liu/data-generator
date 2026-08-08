/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.util.List;

/**
 * Console request for capped {@code geo_synthetic} point sampling preview (D-08).
 *
 * <p>Field semantics mirror {@code GeoSyntheticSourceVO}; {@code maxCount} is the honesty sample
 * size and must be {@code ≤ 500}. Nested {@code sample} mirrors YAML {@code GeoSyntheticSampleVO}
 * for {@code LINE_SAMPLE} honesty (strategy / spacingMeters).
 *
 * @param mode              generation mode ({@code BBOX}, {@code CIRCLE}, {@code BOUNDARY_POINTS}, {@code LINE_SAMPLE})
 * @param seed              deterministic seed (defaults to {@code 0} when null)
 * @param maxCount          requested sample size (hard-capped at 500)
 * @param boundaryPath      boundary GeoJSON path/classpath/{@code asset:} location
 * @param boundaryAssetId   boundary asset UUID (normalized to {@code asset:} at map time)
 * @param networkPath       network GeoJSON path/classpath/{@code asset:} location
 * @param networkAssetId    network asset UUID
 * @param featureIndex      feature index for multi-feature GeoJSON
 * @param randomFeature     whether to pick a random feature
 * @param bbox              {@code [minLon, minLat, maxLon, maxLat]} for BBOX mode
 * @param center            {@code [lon, lat]} for CIRCLE mode
 * @param radiusMeters      circle radius
 * @param minDistanceMeters optional minimum distance between points
 * @param sample            nested LINE_SAMPLE options ({@code strategy}, {@code spacingMeters}); may be null
 * @author Gensokyo
 * @since 2026-08-06
 */
public record GeoSyntheticPreviewRequest(
        String mode,
        Long seed,
        Integer maxCount,
        String boundaryPath,
        String boundaryAssetId,
        String networkPath,
        String networkAssetId,
        Integer featureIndex,
        Boolean randomFeature,
        List<Double> bbox,
        List<Double> center,
        Double radiusMeters,
        Double minDistanceMeters,
        Sample sample) {

    /**
     * Nested LINE_SAMPLE options mirroring {@code GeoSyntheticSampleVO} / YAML {@code sample}.
     *
     * @param strategy       {@code BY_COUNT} or {@code BY_SPACING_METERS}
     * @param spacingMeters  spacing when strategy is {@code BY_SPACING_METERS}
     */
    public record Sample(String strategy, Double spacingMeters) {
    }
}
