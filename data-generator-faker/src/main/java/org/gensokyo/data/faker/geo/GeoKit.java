/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.faker.geo;

import org.gensokyo.data.geo.generate.BoundaryGeometryNormalizer;
import org.gensokyo.data.geo.generate.BoundaryPointGenerator;
import org.gensokyo.data.geo.io.GeoJsonLoader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Geospatial synthesis helpers delegated to {@code data-generator-geo}.
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/8 , Version 1.0.0
 */
public final class GeoKit {

    private GeoKit() {
    }

    /**
     * Parses GeoJSON from a filesystem path and generates random boundary points inside the polygonal area.
     *
     * @param geoJsonPath       GeoJSON path (filesystem)
     * @param featureIndex      feature index inside a FeatureCollection
     * @param count             target point count
     * @param minDistanceMeters minimum spacing between points in meters (&lt;= 0 disables)
     * @param seed              random seed
     * @return generated points (WGS84, x=longitude, y=latitude)
     * @throws IOException when GeoJSON cannot be read
     */
    public static List<Point> generateRandomPointsFromGeoJson(
            Path geoJsonPath,
            int featureIndex,
            int count,
            double minDistanceMeters,
            long seed)
            throws IOException {
        Geometry geometry = GeoJsonLoader.loadGeometry(geoJsonPath.toAbsolutePath().normalize().toString(), featureIndex);
        Geometry normalized = BoundaryGeometryNormalizer.normalize(geometry);
        return BoundaryPointGenerator.generate(normalized, count, minDistanceMeters, seed);
    }
}
