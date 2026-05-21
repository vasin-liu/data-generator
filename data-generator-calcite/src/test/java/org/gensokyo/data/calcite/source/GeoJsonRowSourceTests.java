/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.model.v2.GeoJsonSourceOutputVO;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GeoJsonRowSource} (Phase B file-backed GeoJSON).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class GeoJsonRowSourceTests {

    @Test
    void materializesFeatureCollectionColumns() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");
        GeoJsonRowSource rowSource = new GeoJsonRowSource("gfc", source);
        Assertions.assertEquals(2, rowSource.rows().size());
        Assertions.assertEquals(22.1, rowSource.rows().get(0).values().get("lat"));
        Assertions.assertEquals(113.1, rowSource.rows().get(0).values().get("lon"));
    }

    @Test
    void respectsMaxRows() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");
        source.setMaxRows(1L);
        GeoJsonRowSource rowSource = new GeoJsonRowSource("gfc", source);
        Assertions.assertEquals(1, rowSource.rows().size());
    }

    @Test
    void includePropertiesAddsPropPrefix() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");
        GeoJsonSourceOutputVO output = new GeoJsonSourceOutputVO();
        output.setFormat(GeoOutputFormatKind.columns);
        output.setIncludeProperties(true);
        source.setOutput(output);
        GeoJsonRowSource rowSource = new GeoJsonRowSource("gfc", source);
        Assertions.assertEquals(1.0, rowSource.rows().get(0).values().get("prop.id"));
    }
}
