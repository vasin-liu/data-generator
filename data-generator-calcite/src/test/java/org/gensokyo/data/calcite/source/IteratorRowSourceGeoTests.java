/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.iterator.GeoIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Finite materialization coverage for GEO iterator rows.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class IteratorRowSourceGeoTests {

    @Test
    void materializesGeoBoundaryColumns() {
        GeoIteratorVO geo = new GeoIteratorVO();
        geo.setType("GEO");
        geo.setMode("BOUNDARY_POINTS");
        geo.setBoundaryPath("classpath:geo/南沙区边界.geojson");
        geo.setFeatureIndex(0);
        geo.setCount(6);
        geo.setSeed(11L);

        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(geo);

        IteratorRowSource rowSource = new IteratorRowSource("geo_in", source);
        Assertions.assertEquals(6, rowSource.rows().size());
        Assertions.assertFalse(rowSource.schema().getColumns().isEmpty());
        Assertions.assertTrue(rowSource.schema().getColumns().stream().anyMatch(c -> "lat".equals(c.getName())));
    }
}
