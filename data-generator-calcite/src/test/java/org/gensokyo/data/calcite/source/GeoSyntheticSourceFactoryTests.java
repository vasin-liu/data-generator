/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import java.util.List;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.gensokyo.data.model.v2.GeoSyntheticSampleVO;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GeoSyntheticSourceFactory} supports/create wiring (D-13).
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
class GeoSyntheticSourceFactoryTests {

    private final GeoSyntheticSourceFactory factory = new GeoSyntheticSourceFactory();

    @Test
    void supportsGeoSyntheticSourceVo() {
        Assertions.assertTrue(factory.supports(new GeoSyntheticSourceVO()));
    }

    @Test
    void doesNotSupportGeoJsonSourceVo() {
        Assertions.assertFalse(factory.supports(new GeoJsonSourceVO()));
    }

    @Test
    void createReturnsGeoSyntheticRowSource() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BBOX");
        source.setBbox(List.of(113.2d, 23.0d, 113.5d, 23.2d));
        source.setCount(1);

        var rowSource = factory.create("pts", source);
        Assertions.assertInstanceOf(GeoSyntheticRowSource.class, rowSource);
    }
}
