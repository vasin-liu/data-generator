/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.geo.format.GeoOutputColumnNames;

/**
 * Immutable configuration for one synthetic geospatial generation run.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class GeoGenerationRequest {

    private static final long MAX_COUNT = 1_000_000L;

    private GeoGenerationMode mode;
    private String boundaryPath;
    private String networkPath;
    private int featureIndex;
    private boolean randomFeature;
    private int count;
    private long seed;
    private double minDistanceMeters;
    private GeoSampleStrategyKind sampleStrategy;
    private double spacingMeters;
    private GeoOutputFormatKind outputFormat = GeoOutputFormatKind.columns;
    private GeoOutputColumnNames columnNames = new GeoOutputColumnNames();
    private boolean includeProperties;

    /**
     * Validates configuration and normalizes defaults.
     */
    public void validate() {
        if (mode == null) {
            throw new IllegalArgumentException("GEO iterator mode must be set");
        }
        if (count > MAX_COUNT) {
            throw new IllegalArgumentException("GEO iterator count must be <= " + MAX_COUNT);
        }
        switch (mode) {
            case BOUNDARY_POINTS -> validateBoundary();
            case LINE_SAMPLE -> validateLineSample();
        }
    }

    private void validateBoundary() {
        if (boundaryPath == null || boundaryPath.isBlank()) {
            throw new IllegalArgumentException("BOUNDARY_POINTS requires boundaryPath");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("BOUNDARY_POINTS requires count > 0");
        }
        if (minDistanceMeters < 0) {
            throw new IllegalArgumentException("minDistanceMeters must be >= 0");
        }
    }

    private void validateLineSample() {
        if (networkPath == null || networkPath.isBlank()) {
            throw new IllegalArgumentException("LINE_SAMPLE requires networkPath");
        }
        if (sampleStrategy == null) {
            throw new IllegalArgumentException("LINE_SAMPLE requires sample.strategy");
        }
        if (sampleStrategy == GeoSampleStrategyKind.BY_COUNT && count <= 0) {
            throw new IllegalArgumentException("BY_COUNT requires count > 0");
        }
        if (sampleStrategy == GeoSampleStrategyKind.BY_SPACING_METERS && spacingMeters <= 0) {
            throw new IllegalArgumentException("BY_SPACING_METERS requires spacingMeters > 0");
        }
    }

    public GeoGenerationMode getMode() {
        return mode;
    }

    public void setMode(GeoGenerationMode mode) {
        this.mode = mode;
    }

    public String getBoundaryPath() {
        return boundaryPath;
    }

    public void setBoundaryPath(String boundaryPath) {
        this.boundaryPath = boundaryPath;
    }

    public String getNetworkPath() {
        return networkPath;
    }

    public void setNetworkPath(String networkPath) {
        this.networkPath = networkPath;
    }

    public int getFeatureIndex() {
        return featureIndex;
    }

    public void setFeatureIndex(int featureIndex) {
        this.featureIndex = featureIndex;
    }

    public boolean isRandomFeature() {
        return randomFeature;
    }

    public void setRandomFeature(boolean randomFeature) {
        this.randomFeature = randomFeature;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public double getMinDistanceMeters() {
        return minDistanceMeters;
    }

    public void setMinDistanceMeters(double minDistanceMeters) {
        this.minDistanceMeters = minDistanceMeters;
    }

    public GeoSampleStrategyKind getSampleStrategy() {
        return sampleStrategy;
    }

    public void setSampleStrategy(GeoSampleStrategyKind sampleStrategy) {
        this.sampleStrategy = sampleStrategy;
    }

    public double getSpacingMeters() {
        return spacingMeters;
    }

    public void setSpacingMeters(double spacingMeters) {
        this.spacingMeters = spacingMeters;
    }

    public GeoOutputFormatKind getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(GeoOutputFormatKind outputFormat) {
        if (outputFormat != null) {
            this.outputFormat = outputFormat;
        }
    }

    public GeoOutputColumnNames getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(GeoOutputColumnNames columnNames) {
        if (columnNames != null) {
            this.columnNames = columnNames;
        }
    }

    public boolean isIncludeProperties() {
        return includeProperties;
    }

    public void setIncludeProperties(boolean includeProperties) {
        this.includeProperties = includeProperties;
    }
}
