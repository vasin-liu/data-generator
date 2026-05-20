/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.generate;

import org.gensokyo.data.geo.GeoHaversine;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

/**
 * Selects a single {@link LineString} component from network feature geometry.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class LineComponentSelector {

    private LineComponentSelector() {
    }

    /**
     * Returns the longest {@link LineString} in the geometry (or the line itself).
     *
     * @param geometry feature geometry
     * @return longest line string
     */
    public static LineString selectLongestLineString(Geometry geometry) {
        if (geometry instanceof LineString lineString) {
            if (lineString.getNumPoints() < 2) {
                throw new IllegalArgumentException("LineString must contain at least two coordinates");
            }
            return lineString;
        }
        if (geometry instanceof MultiLineString multiLineString) {
            LineString longest = null;
            double maxLength = -1d;
            for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
                LineString candidate = (LineString) multiLineString.getGeometryN(i);
                double length = GeoHaversine.lineLengthMeters(candidate);
                if (length > maxLength) {
                    maxLength = length;
                    longest = candidate;
                }
            }
            if (longest == null) {
                throw new IllegalArgumentException("MultiLineString has no line components");
            }
            return longest;
        }
        throw new IllegalArgumentException("Unsupported network geometry type for LINE_SAMPLE: "
                + geometry.getGeometryType());
    }
}
