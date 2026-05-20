/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.geo.GeoGenerationMode;
import org.gensokyo.data.geo.GeoSampleStrategyKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link GeoIteratorRequestMapper}.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
class GeoIteratorRequestMapperTests {

    @Test
    void mapsLineSampleStrategy() {
        GeoIteratorVO vo = new GeoIteratorVO();
        vo.setType("GEO");
        vo.setMode("LINE_SAMPLE");
        vo.setNetworkPath("classpath:geo/南沙区道路路网.geojson");
        vo.setFeatureIndex(0);
        vo.setCount(10);
        vo.setSeed(2L);
        GeoSampleConfigVO sample = new GeoSampleConfigVO();
        sample.setStrategy("BY_COUNT");
        vo.setSample(sample);

        var request = GeoIteratorRequestMapper.toRequest(vo);
        Assertions.assertEquals(GeoGenerationMode.LINE_SAMPLE, request.getMode());
        Assertions.assertEquals(GeoSampleStrategyKind.BY_COUNT, request.getSampleStrategy());
    }
}
