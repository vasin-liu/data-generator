/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.geo.GeoGenerationMode;
import org.gensokyo.data.geo.GeoGenerationRequest;
import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.GeoSampleStrategyKind;

/**
 * Maps {@link GeoIteratorVO} to {@link GeoGenerationRequest}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoIteratorRequestMapper {

    private GeoIteratorRequestMapper() {
    }

    /**
     * Converts iterator VO configuration to a generation request.
     *
     * @param iterator geo iterator VO
     * @return generation request
     */
    public static GeoGenerationRequest toRequest(GeoIteratorVO iterator) {
        GeoGenerationRequest request = new GeoGenerationRequest();
        request.setMode(parseMode(iterator.getMode()));
        request.setBoundaryPath(iterator.getBoundaryPath());
        request.setNetworkPath(iterator.getNetworkPath());
        request.setFeatureIndex(iterator.getFeatureIndex());
        request.setRandomFeature(iterator.isRandomFeature());
        request.setCount(iterator.getCount());
        request.setSeed(iterator.getSeed());
        request.setMinDistanceMeters(iterator.getMinDistanceMeters());
        if (iterator.getSample() != null && iterator.getSample().getStrategy() != null) {
            request.setSampleStrategy(parseSampleStrategy(iterator.getSample().getStrategy()));
            request.setSpacingMeters(iterator.getSample().getSpacingMeters());
        }
        if (iterator.getOutput() != null) {
            request.setOutputFormat(parseOutputFormat(iterator.getOutput().getFormat()));
            request.setColumnNames(iterator.getOutput().getColumnNames());
            request.setIncludeProperties(iterator.getOutput().isIncludeProperties());
        }
        return request;
    }

    private static GeoGenerationMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("GEO iterator mode must be set");
        }
        return GeoGenerationMode.valueOf(mode.strip().toUpperCase());
    }

    private static GeoSampleStrategyKind parseSampleStrategy(String strategy) {
        return GeoSampleStrategyKind.valueOf(strategy.strip().toUpperCase());
    }

    private static GeoOutputFormatKind parseOutputFormat(String format) {
        if (format == null || format.isBlank()) {
            return GeoOutputFormatKind.columns;
        }
        return GeoOutputFormatKind.valueOf(format.strip().toLowerCase());
    }
}
