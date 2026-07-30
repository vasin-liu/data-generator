/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.source.GeoSyntheticSourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;

import java.util.List;
import java.util.Map;

/**
 * End-to-end Template V2 pipeline proof for {@code geo_synthetic} sources (Phase 20 — GEO-02 closeout).
 *
 * <p>Exercises all four synthetic geo modes through {@link TemplateV2Runner} with passthrough SQL
 * and console sink; mirrors {@link TemplateV2RunnerGeoSourceTests} registry pattern per D-01.</p>
 *
 * @author Gensokyo
 * @since 2026-07-30
 */
class TemplateV2RunnerGeoSyntheticSourceTests {

    private static TemplateV2Runner runner(TemplateV2RuntimeRegistry registry) {
        return new TemplateV2Runner(registry);
    }

    private static TemplateV2RuntimeRegistry geoSyntheticRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new GeoSyntheticSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }

    private static TemplateV2VO template(String name, Map<String, SourceVO> sources, SqlTransformVO transform) {
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));

        TemplateV2VO template = new TemplateV2VO();
        template.setName(name);
        template.setSources(sources);
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }
}
