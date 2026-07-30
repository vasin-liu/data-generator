/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.geo.format.GeoOutputColumnNames;

/**
 * Immutable configuration for one synthetic geospatial generation run.
 * <p>
 * Callers must always supply an explicit {@link #seed}; {@code 0} is a valid deterministic seed (D-06).
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
    private double bboxMinLon;
    private double bboxMinLat;
    private double bboxMaxLon;
    private double bboxMaxLat;
    private double centerLon;
    private double centerLat;
    private double radiusMeters;
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
            case BBOX -> validateBbox();
            case CIRCLE -> validateCircle();
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

    private void validateBbox() {
        if (count <= 0) {
            throw new IllegalArgumentException("BBOX requires count > 0");
        }
        if (minDistanceMeters < 0) {
            throw new IllegalArgumentException("minDistanceMeters must be >= 0");
        }
        if (bboxMinLon >= bboxMaxLon) {
            throw new IllegalArgumentException("bboxMinLon must be less than bboxMaxLon");
        }
        if (bboxMinLat >= bboxMaxLat) {
            throw new IllegalArgumentException("bboxMinLat must be less than bboxMaxLat");
        }
    }

    private void validateCircle() {
        if (count <= 0) {
            throw new IllegalArgumentException("CIRCLE requires count > 0");
        }
        if (!Double.isFinite(centerLon)) {
            throw new IllegalArgumentException("centerLon must be finite");
        }
        if (!Double.isFinite(centerLat)) {
            throw new IllegalArgumentException("centerLat must be finite");
        }
        if (centerLon < -180d || centerLon > 180d) {
            throw new IllegalArgumentException("centerLon must be within [-180, 180]");
        }
        if (centerLat < -90d || centerLat > 90d) {
            throw new IllegalArgumentException("centerLat must be within [-90, 90]");
        }
        if (radiusMeters <= 0d) {
            throw new IllegalArgumentException("radiusMeters must be > 0");
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

    /**
     * Deterministic seed for point sampling; {@code 0} is valid.
     *
     * @return explicit seed supplied by the caller
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Sets the deterministic seed; callers must always provide an explicit value.
     *
     * @param seed seed value ({@code 0} is valid)
     */
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

    /**
     * Minimum longitude (WGS84 degrees) for {@link GeoGenerationMode#BBOX}.
     *
     * @return western bbox edge in degrees
     */
    public double getBboxMinLon() {
        return bboxMinLon;
    }

    /**
     * Sets the minimum longitude for BBOX mode.
     *
     * @param bboxMinLon western bbox edge in degrees
     */
    public void setBboxMinLon(double bboxMinLon) {
        this.bboxMinLon = bboxMinLon;
    }

    /**
     * Minimum latitude (WGS84 degrees) for {@link GeoGenerationMode#BBOX}.
     *
     * @return southern bbox edge in degrees
     */
    public double getBboxMinLat() {
        return bboxMinLat;
    }

    /**
     * Sets the minimum latitude for BBOX mode.
     *
     * @param bboxMinLat southern bbox edge in degrees
     */
    public void setBboxMinLat(double bboxMinLat) {
        this.bboxMinLat = bboxMinLat;
    }

    /**
     * Maximum longitude (WGS84 degrees) for {@link GeoGenerationMode#BBOX}.
     *
     * @return eastern bbox edge in degrees
     */
    public double getBboxMaxLon() {
        return bboxMaxLon;
    }

    /**
     * Sets the maximum longitude for BBOX mode.
     *
     * @param bboxMaxLon eastern bbox edge in degrees
     */
    public void setBboxMaxLon(double bboxMaxLon) {
        this.bboxMaxLon = bboxMaxLon;
    }

    /**
     * Maximum latitude (WGS84 degrees) for {@link GeoGenerationMode#BBOX}.
     *
     * @return northern bbox edge in degrees
     */
    public double getBboxMaxLat() {
        return bboxMaxLat;
    }

    /**
     * Sets the maximum latitude for BBOX mode.
     *
     * @param bboxMaxLat northern bbox edge in degrees
     */
    public void setBboxMaxLat(double bboxMaxLat) {
        this.bboxMaxLat = bboxMaxLat;
    }

    /**
     * Center longitude (WGS84 degrees) for {@link GeoGenerationMode#CIRCLE}.
     *
     * @return center longitude in degrees
     */
    public double getCenterLon() {
        return centerLon;
    }

    /**
     * Sets the center longitude for CIRCLE mode.
     *
     * @param centerLon center longitude in degrees
     */
    public void setCenterLon(double centerLon) {
        this.centerLon = centerLon;
    }

    /**
     * Center latitude (WGS84 degrees) for {@link GeoGenerationMode#CIRCLE}.
     *
     * @return center latitude in degrees
     */
    public double getCenterLat() {
        return centerLat;
    }

    /**
     * Sets the center latitude for CIRCLE mode.
     *
     * @param centerLat center latitude in degrees
     */
    public void setCenterLat(double centerLat) {
        this.centerLat = centerLat;
    }

    /**
     * Sampling radius in meters for {@link GeoGenerationMode#CIRCLE}.
     *
     * @return radius in meters
     */
    public double getRadiusMeters() {
        return radiusMeters;
    }

    /**
     * Sets the sampling radius for CIRCLE mode.
     *
     * @param radiusMeters radius in meters
     */
    public void setRadiusMeters(double radiusMeters) {
        this.radiusMeters = radiusMeters;
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
