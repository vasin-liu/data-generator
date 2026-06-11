/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.faker.geo;

import org.gensokyo.data.geo.GeoGenerationMode;
import org.gensokyo.data.geo.GeoGenerationRequest;
import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.geo.GeoSyntheticGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Boundary point helpers for SpEL and {@link org.gensokyo.data.faker.GeoProvider}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoSpelSupport {

    /**
     * Returns {@code count} synthetic points fully inside {@code boundaryPath}.
     *
     * @param boundaryPath             classpath ({@code classpath:}) or filesystem path to GeoJSON
     * @param count                    positive point count
     * @param minDistanceMeters        minimum spacing in meters (&lt;= 0 disables)
     * @param seed                     random seed
     * @return row maps aligned with GEO iterator {@code columns} format ({@code lat}, {@code lon})
     * @throws IOException when GeoJSON cannot be read
     */
    public List<Map<String, Object>> pointsInBoundary(
            String boundaryPath,
            int count,
            double minDistanceMeters,
            long seed)
            throws IOException {
        GeoGenerationRequest request = new GeoGenerationRequest();
        request.setMode(GeoGenerationMode.BOUNDARY_POINTS);
        request.setBoundaryPath(boundaryPath);
        request.setCount(count);
        request.setMinDistanceMeters(minDistanceMeters);
        request.setSeed(seed);
        request.setOutputFormat(GeoOutputFormatKind.columns);
        request.validate();
        return GeoSyntheticGenerator.generateRows(request);
    }

    /**
     * Returns one random point inside {@code boundaryPath}.
     *
     * @param boundaryPath classpath or filesystem path to GeoJSON
     * @param seed         random seed
     * @return single-row map ({@code lat}, {@code lon})
     * @throws IOException when GeoJSON cannot be read
     */
    public Map<String, Object> randomPointInBoundary(String boundaryPath, long seed) throws IOException {
        List<Map<String, Object>> rows = pointsInBoundary(boundaryPath, 1, 0d, seed);
        return rows.get(0);
    }
}
