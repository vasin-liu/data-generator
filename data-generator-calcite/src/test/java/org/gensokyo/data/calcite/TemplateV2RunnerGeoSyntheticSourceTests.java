/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.source.GeoSyntheticSourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.model.v2.GeoSyntheticSampleVO;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    private static final String BOUNDARY_FIXTURE = "classpath:geo/南沙区边界.geojson";
    private static final String NETWORK_FIXTURE = "classpath:geo/南沙区道路路网.geojson";

    @Test
    void boundaryPoints_pipelineRun_returnsExpectedRowCount() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BOUNDARY_POINTS");
        source.setBoundaryPath(BOUNDARY_FIXTURE);
        source.setCount(6);
        source.setSeed(11L);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lon, lat from geo_boundary");

        TemplateV2VO template = template("geo-synthetic-boundary", Map.of("geo_boundary", source), transform);

        TemplateV2RunResult result = runner(geoSyntheticRegistry()).run(template);

        Assertions.assertEquals(6, result.getRows().size());
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lat")));
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lon")));
    }

    @Test
    void lineSample_pipelineRun_returnsNonEmptyRows() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("LINE_SAMPLE");
        source.setNetworkPath(NETWORK_FIXTURE);
        GeoSyntheticSampleVO sample = new GeoSyntheticSampleVO();
        sample.setStrategy("BY_SPACING_METERS");
        sample.setSpacingMeters(50d);
        source.setSample(sample);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lon, lat from geo_line");

        TemplateV2VO template = template("geo-synthetic-line", Map.of("geo_line", source), transform);

        TemplateV2RunResult result = runner(geoSyntheticRegistry()).run(template);

        Assertions.assertFalse(result.getRows().isEmpty());
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lat")));
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lon")));
    }

    @Test
    void bbox_pipelineRun_returnsExpectedRowCount() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BBOX");
        source.setBbox(List.of(113.2d, 23.0d, 113.5d, 23.2d));
        source.setCount(5);
        source.setSeed(42L);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lon, lat from geo_bbox");

        TemplateV2VO template = template("geo-synthetic-bbox", Map.of("geo_bbox", source), transform);

        TemplateV2RunResult result = runner(geoSyntheticRegistry()).run(template);

        Assertions.assertEquals(5, result.getRows().size());
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lat")));
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lon")));
    }

    @Test
    void circle_pipelineRun_returnsExpectedRowCount() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("CIRCLE");
        source.setCenter(List.of(113.3d, 23.1d));
        source.setRadiusMeters(500d);
        source.setCount(4);
        source.setSeed(7L);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lon, lat from geo_circle");

        TemplateV2VO template = template("geo-synthetic-circle", Map.of("geo_circle", source), transform);

        TemplateV2RunResult result = runner(geoSyntheticRegistry()).run(template);

        Assertions.assertEquals(4, result.getRows().size());
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lat")));
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lon")));
    }

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
