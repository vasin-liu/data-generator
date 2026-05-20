/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.generate;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes boundary geometries for point-in-polygon sampling.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class BoundaryGeometryNormalizer {

    private BoundaryGeometryNormalizer() {
    }

    /**
     * Returns a polygonal geometry suitable for {@link BoundaryPointGenerator}.
     *
     * @param geometry loaded boundary geometry
     * @return polygon or multipolygon (or union of polygonal parts)
     */
    public static Geometry normalize(Geometry geometry) {
        if (geometry == null) {
            throw new IllegalArgumentException("Boundary geometry must not be null");
        }
        if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
            return geometry;
        }
        if (geometry instanceof GeometryCollection collection) {
            List<Geometry> polygons = new ArrayList<>();
            collectPolygons(collection, polygons);
            if (polygons.isEmpty()) {
                throw new IllegalArgumentException("GeometryCollection contains no polygonal components for boundary sampling");
            }
            return UnaryUnionOp.union(polygons);
        }
        throw new IllegalArgumentException("Unsupported boundary geometry type: " + geometry.getGeometryType());
    }

    private static void collectPolygons(Geometry geometry, List<Geometry> polygons) {
        if (geometry instanceof Polygon polygon) {
            polygons.add(polygon);
            return;
        }
        if (geometry instanceof MultiPolygon multiPolygon) {
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                polygons.add(multiPolygon.getGeometryN(i));
            }
            return;
        }
        if (geometry instanceof GeometryCollection collection) {
            for (int i = 0; i < collection.getNumGeometries(); i++) {
                collectPolygons(collection.getGeometryN(i), polygons);
            }
        }
    }
}
