/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import java.util.List;
import org.gensokyo.data.geo.GeoGenerationMode;
import org.gensokyo.data.geo.GeoGenerationRequest;
import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.GeoSampleStrategyKind;
import org.gensokyo.data.model.v2.GeoSyntheticSourceOutputVO;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;

/**
 * Maps Template V2 {@link GeoSyntheticSourceVO} to {@link GeoGenerationRequest} (D-04).
 *
 * <p>Expands YAML {@code bbox} / {@code center} arrays into flat request fields; path resolution
 * stays in {@code GeoSyntheticGenerator} via {@code GeoResourceResolver} (GEO-03 / D-08).</p>
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
public final class GeoSyntheticRequestMapper {

    private GeoSyntheticRequestMapper() {
    }

    /**
     * Converts a geo synthetic source VO to a validated generation request.
     *
     * @param sourceName logical Template V2 source name (included in validation errors per D-07)
     * @param source     geo synthetic source configuration
     * @return validated {@link GeoGenerationRequest}
     * @throws IllegalArgumentException when mapping or {@link GeoGenerationRequest#validate()} fails
     */
    public static GeoGenerationRequest toRequest(String sourceName, GeoSyntheticSourceVO source) {
        GeoGenerationMode mode = parseMode(sourceName, source.getMode());
        GeoGenerationRequest request = new GeoGenerationRequest();
        request.setMode(mode);
        request.setBoundaryPath(source.getBoundaryPath());
        request.setNetworkPath(source.getNetworkPath());
        request.setFeatureIndex(source.getFeatureIndex());
        request.setRandomFeature(source.isRandomFeature());
        request.setCount(source.getCount());
        request.setSeed(source.getSeed());
        request.setMinDistanceMeters(source.getMinDistanceMeters());
        request.setRadiusMeters(source.getRadiusMeters());

        // Expand mode-specific YAML arrays before path and validate checks.
        switch (mode) {
            case BBOX -> expandBbox(sourceName, source.getBbox(), request);
            case CIRCLE -> expandCenter(sourceName, source.getCenter(), request);
            default -> {
                // BOUNDARY_POINTS / LINE_SAMPLE use scalar path fields only.
            }
        }

        if (source.getSample() != null && source.getSample().getStrategy() != null) {
            request.setSampleStrategy(parseSampleStrategy(source.getSample().getStrategy()));
            request.setSpacingMeters(source.getSample().getSpacingMeters());
        }
        if (source.getOutput() != null) {
            GeoSyntheticSourceOutputVO output = source.getOutput();
            request.setOutputFormat(resolveOutputFormat(output.getFormat()));
            request.setColumnNames(output.getColumnNames());
            request.setIncludeProperties(output.isIncludeProperties());
        }

        enforceModePaths(sourceName, mode, source);

        try {
            request.validate();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "GEO synthetic source [" + sourceName + "]: " + ex.getMessage(), ex);
        }
        return request;
    }

    private static void enforceModePaths(String sourceName, GeoGenerationMode mode, GeoSyntheticSourceVO source) {
        switch (mode) {
            case BOUNDARY_POINTS -> {
                if (source.getBoundaryPath() == null || source.getBoundaryPath().isBlank()) {
                    throw new IllegalArgumentException(
                            "boundaryPath must not be blank for GEO synthetic source [" + sourceName + "]");
                }
            }
            case LINE_SAMPLE -> {
                if (source.getNetworkPath() == null || source.getNetworkPath().isBlank()) {
                    throw new IllegalArgumentException(
                            "networkPath must not be blank for GEO synthetic source [" + sourceName + "]");
                }
            }
            default -> {
                // BBOX / CIRCLE do not require boundary or network paths.
            }
        }
    }

    private static void expandBbox(String sourceName, List<Double> bbox, GeoGenerationRequest request) {
        if (bbox == null || bbox.size() != 4) {
            throw new IllegalArgumentException(
                    "bbox requires exactly 4 elements [minLon, minLat, maxLon, maxLat] for GEO synthetic source ["
                            + sourceName + "]");
        }
        double minLon = requireFinite(sourceName, "bbox", bbox.get(0), 0);
        double minLat = requireFinite(sourceName, "bbox", bbox.get(1), 1);
        double maxLon = requireFinite(sourceName, "bbox", bbox.get(2), 2);
        double maxLat = requireFinite(sourceName, "bbox", bbox.get(3), 3);
        request.setBboxMinLon(minLon);
        request.setBboxMinLat(minLat);
        request.setBboxMaxLon(maxLon);
        request.setBboxMaxLat(maxLat);
    }

    private static void expandCenter(String sourceName, List<Double> center, GeoGenerationRequest request) {
        if (center == null || center.size() != 2) {
            throw new IllegalArgumentException(
                    "center requires exactly 2 elements [lon, lat] for GEO synthetic source [" + sourceName + "]");
        }
        request.setCenterLon(requireFinite(sourceName, "center", center.get(0), 0));
        request.setCenterLat(requireFinite(sourceName, "center", center.get(1), 1));
    }

    private static double requireFinite(String sourceName, String field, Double value, int index) {
        if (value == null || !Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    field + "[" + index + "] must be a finite number for GEO synthetic source [" + sourceName + "]");
        }
        return value;
    }

    private static GeoGenerationMode parseMode(String sourceName, String mode) {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException(
                    "mode must be set for GEO synthetic source [" + sourceName + "]");
        }
        try {
            return GeoGenerationMode.valueOf(mode.strip().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "mode [" + mode + "] is invalid for GEO synthetic source [" + sourceName + "]", ex);
        }
    }

    private static GeoSampleStrategyKind parseSampleStrategy(String strategy) {
        return GeoSampleStrategyKind.valueOf(strategy.strip().toUpperCase());
    }

    private static GeoOutputFormatKind resolveOutputFormat(GeoOutputFormatKind format) {
        return format == null ? GeoOutputFormatKind.columns : format;
    }
}
