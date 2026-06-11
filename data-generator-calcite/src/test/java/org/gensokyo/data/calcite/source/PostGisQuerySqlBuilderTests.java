/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.model.v2.GeoJsonSourceOutputVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for {@link PostGisQuerySqlBuilder}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class PostGisQuerySqlBuilderTests {

    @Test
    void buildsColumnsProjection() {
        PostGisQuerySourceVO source = baseSource();
        source.getOutput().setFormat(GeoOutputFormatKind.columns);
        String sql = PostGisQuerySqlBuilder.buildSelect(source);
        Assertions.assertTrue(sql.contains("ST_Y(ST_PointOnSurface(sites.geom)) AS lat"));
        Assertions.assertTrue(sql.contains("FROM sites"));
    }

    @Test
    void buildsWktWithPropertyAlias() {
        PostGisQuerySourceVO source = baseSource();
        source.getOutput().setFormat(GeoOutputFormatKind.wkt);
        source.getOutput().setIncludeProperties(true);
        source.setAttributes(List.of("id"));
        String sql = PostGisQuerySqlBuilder.buildSelect(source);
        Assertions.assertTrue(sql.contains("ST_AsText(sites.geom) AS geometry"));
        Assertions.assertTrue(sql.contains("sites.id AS \"prop.id\""));
    }

    @Test
    void rejectsUnsafeWhereClause() {
        PostGisQuerySourceVO source = baseSource();
        source.setWhere("1=1; drop table sites");
        Assertions.assertThrows(IllegalArgumentException.class, () -> PostGisQuerySqlBuilder.buildSelect(source));
    }

    private static PostGisQuerySourceVO baseSource() {
        PostGisQuerySourceVO source = new PostGisQuerySourceVO();
        source.setTable("sites");
        source.setGeometryColumn("geom");
        source.setOutput(new GeoJsonSourceOutputVO());
        return source;
    }
}
