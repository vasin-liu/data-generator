/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

import org.gensokyo.data.calcite.TemplateV2SqlFunctionContext;
import org.gensokyo.data.geo.GeoHaversine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link TemplateV2GeoSqlFunctions}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class TemplateV2GeoSqlFunctionsTests {

    private static final String BOX =
            "POLYGON((113.15 22.15, 113.25 22.15, 113.25 22.25, 113.15 22.25, 113.15 22.15))";

    @Test
    void distanceMetersMatchesGeoHaversine() {
        double expected = GeoHaversine.distanceMeters(22.1, 113.1, 22.2, 113.2);
        Double actual = TemplateV2GeoSqlFunctions.distanceMeters(
                new TemplateV2SqlFunctionContext(List.of(22.1, 113.1, 22.2, 113.2)));

        Assertions.assertEquals(expected, actual, 0.01);
        Assertions.assertTrue(actual > 10_000 && actual < 20_000);
    }

    @Test
    void distanceMetersReturnsNullWhenAnyArgumentNull() {
        List<Object> args = new ArrayList<>();
        args.add(22.1);
        args.add(null);
        args.add(22.2);
        args.add(113.2);
        Assertions.assertNull(TemplateV2GeoSqlFunctions.distanceMeters(new TemplateV2SqlFunctionContext(args)));
    }

    @Test
    void withinRadiusIsTrueAtCenterAndFalseBeyondRadius() {
        Assertions.assertTrue(TemplateV2GeoSqlFunctions.withinRadius(
                new TemplateV2SqlFunctionContext(List.of(22.2, 113.2, 22.2, 113.2, 1))));
        Assertions.assertFalse(TemplateV2GeoSqlFunctions.withinRadius(
                new TemplateV2SqlFunctionContext(List.of(22.1, 113.1, 22.2, 113.2, 5_000))));
    }

    @Test
    void withinRadiusReturnsNullWhenAnyArgumentNull() {
        List<Object> args = new ArrayList<>();
        args.add(22.1);
        args.add(113.1);
        args.add(22.2);
        args.add(null);
        args.add(5_000);
        Assertions.assertNull(TemplateV2GeoSqlFunctions.withinRadius(new TemplateV2SqlFunctionContext(args)));
    }

    @Test
    void pointInWktDelegatesToGeoModule() {
        Assertions.assertTrue(TemplateV2GeoSqlFunctions.pointInWkt(
                new TemplateV2SqlFunctionContext(List.of(22.2, 113.2, BOX))));
        Assertions.assertFalse(TemplateV2GeoSqlFunctions.pointInWkt(
                new TemplateV2SqlFunctionContext(List.of(22.1, 113.1, BOX))));
    }

    @Test
    void wktContainsAndIntersectsDelegateToGeoModule() {
        Assertions.assertTrue(TemplateV2GeoSqlFunctions.wktContains(
                new TemplateV2SqlFunctionContext(List.of(BOX, "POINT(113.2 22.2)"))));
        Assertions.assertTrue(TemplateV2GeoSqlFunctions.wktIntersects(
                new TemplateV2SqlFunctionContext(List.of("POINT(113.2 22.2)", "POINT(113.2 22.2)"))));
    }
}
