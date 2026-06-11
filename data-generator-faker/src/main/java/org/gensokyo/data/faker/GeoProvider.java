/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import org.gensokyo.data.faker.geo.GeoSpelSupport;

import java.util.List;
import java.util.Map;

/**
 * Geospatial SpEL namespace exposed as {@code faker.geo()}.
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/8 , Version 1.0.0
 */
public class GeoProvider extends AbstractProvider<BaseProviders> {

    private final GeoSpelSupport geoSpelSupport = new GeoSpelSupport();

    protected GeoProvider(BaseProviders faker) {
        super(faker);
    }

    /**
     * Returns synthetic boundary points compatible with GEO iterator {@code columns} output.
     *
     * @param boundaryPath             classpath or filesystem GeoJSON path
     * @param count                    point count
     * @param minDistanceMeters        minimum spacing (&lt;= 0 disables)
     * @param seed                     random seed
     * @return list of row maps
     */
    public List<Map<String, Object>> pointsInBoundary(
            String boundaryPath,
            int count,
            double minDistanceMeters,
            long seed) {
        try {
            return geoSpelSupport.pointsInBoundary(boundaryPath, count, minDistanceMeters, seed);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns one random point inside the boundary.
     *
     * @param boundaryPath classpath or filesystem GeoJSON path
     * @param seed         random seed
     * @return lat/lon row map
     */
    public Map<String, Object> randomPointInBoundary(String boundaryPath, long seed) {
        try {
            return geoSpelSupport.randomPointInBoundary(boundaryPath, seed);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
