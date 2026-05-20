/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.faker.geo;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.faker.DataFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;

/**
 * SpEL coverage for {@link GeoSpelSupport} via {@code #geo} and {@code #faker.geo}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class GeoSpelSupportTests {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final StandardEvaluationContext context = new StandardEvaluationContext();

    @Test
    void geoVariablePointsInBoundary() {
        context.setVariable(Const.SCRIPT_VAR_GEO, new GeoSpelSupport());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = parser.parseExpression(
                        "#geo.pointsInBoundary('classpath:geo/南沙区边界.geojson', 5, 0, 99L)")
                .getValue(context, List.class);
        Assertions.assertNotNull(rows);
        Assertions.assertEquals(5, rows.size());
    }

    @Test
    void fakerGeoPointsInBoundary() {
        DataFaker faker = new DataFaker();
        context.setVariable(Const.SCRIPT_VAR_FAKER, faker);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = parser.parseExpression(
                        "#faker.geo().pointsInBoundary('classpath:geo/南沙区边界.geojson', 3, 0, 1L)")
                .getValue(context, List.class);
        Assertions.assertEquals(3, rows.size());
    }
}
