/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite;

import org.gensokyo.data.geo.GeoOutputFormatKind;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.source.GeoJsonSourceFactory;
import org.gensokyo.data.calcite.source.IteratorSourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.iterator.GeoIteratorVO;
import org.gensokyo.data.model.v2.GeoJsonSourceOutputVO;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
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
 * End-to-end Calcite SQL over geospatial V2 sources (Phase D — sources registered in execution context).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class TemplateV2RunnerGeoSourceTests {

    @Test
    void runsTemplateWithGeoJsonSourceAndSqlFilter() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lat, lon from geo_in where lat > 22.15");

        TemplateV2VO template = template("geojson-sql", Map.of("geo_in", source), transform);

        TemplateV2RunResult result = runner(geoJsonRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals(22.2, ((Number) result.getRows().get(0).values().get("lat")).doubleValue(), 0.001);
    }

    @Test
    void runsTemplateWithGeoJsonSourceAndGeoDistanceUdf() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("""
                select lat, lon
                from geo_in
                where V2_GEO_WITHIN_RADIUS(lat, lon, 22.2, 113.2, 5000)
                """);

        TemplateV2VO template = template("geojson-distance-udf", Map.of("geo_in", source), transform);

        TemplateV2RunResult result = runner(geoJsonRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals(22.2, ((Number) result.getRows().get(0).values().get("lat")).doubleValue(), 0.001);
    }

    @Test
    void runsTemplateWithGeoJsonSourceAndPointInWktFilter() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("""
                select lat, lon
                from geo_in
                where V2_GEO_POINT_IN_WKT(lat, lon,
                  'POLYGON((113.15 22.15, 113.25 22.15, 113.25 22.25, 113.15 22.25, 113.15 22.15))')
                """);

        TemplateV2VO template = template("geojson-point-in-wkt", Map.of("geo_in", source), transform);

        TemplateV2RunResult result = runner(geoJsonRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertEquals(22.2, ((Number) result.getRows().get(0).values().get("lat")).doubleValue(), 0.001);
    }

    @Test
    void runsTemplateWithGeoJsonWktColumnsAndWktContains() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");
        GeoJsonSourceOutputVO output = new GeoJsonSourceOutputVO();
        output.setFormat(GeoOutputFormatKind.wkt);
        source.setOutput(output);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("""
                select geometry
                from geo_wkt
                where V2_GEO_WKT_CONTAINS(
                  'POLYGON((113.15 22.15, 113.25 22.15, 113.25 22.25, 113.15 22.25, 113.15 22.15))',
                  geometry)
                """);

        TemplateV2VO template = template("geojson-wkt-contains", Map.of("geo_wkt", source), transform);

        TemplateV2RunResult result = runner(geoJsonRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertTrue(result.getRows().get(0).values().get("geometry").toString().contains("POINT"));
    }

    @Test
    void runsTemplateWithGeoJsonGeometryColumnAndGeoJsonContains() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");
        GeoJsonSourceOutputVO output = new GeoJsonSourceOutputVO();
        output.setFormat(GeoOutputFormatKind.geojson);
        source.setOutput(output);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("""
                select geometry
                from geo_gj
                where V2_GEO_GEOJSON_CONTAINS(
                  '{"type":"Polygon","coordinates":[[[113.15,22.15],[113.25,22.15],[113.25,22.25],[113.15,22.25],[113.15,22.15]]]}',
                  geometry)
                """);

        TemplateV2VO template = template("geojson-contains-udf", Map.of("geo_gj", source), transform);

        TemplateV2RunResult result = runner(geoJsonRegistry()).run(template);

        Assertions.assertEquals(1, result.getRows().size());
        Assertions.assertTrue(result.getRows().get(0).values().get("geometry").toString().contains("Point"));
    }

    @Test
    void runsTemplateWithWktBufferAndPointInWktFilter() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setPath("classpath:geo/two_feature_collection.geojson");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("""
                select lat, lon
                from geo_in
                where V2_GEO_POINT_IN_WKT(
                  lat,
                  lon,
                  V2_GEO_WKT_BUFFER('POINT(113.2 22.2)', 20000))
                """);

        TemplateV2VO template = template("geojson-wkt-buffer", Map.of("geo_in", source), transform);

        TemplateV2RunResult result = runner(geoJsonRegistry()).run(template);

        // ~15 km between fixture points; 20 km buffer around (22.2, 113.2) should include both.
        Assertions.assertEquals(2, result.getRows().size());
    }

    @Test
    void runsTemplateWithGeoIteratorSourceAndSqlProjection() {
        GeoIteratorVO geo = new GeoIteratorVO();
        geo.setType("GEO");
        geo.setMode("BOUNDARY_POINTS");
        geo.setBoundaryPath("classpath:geo/南沙区边界.geojson");
        geo.setCount(5);
        geo.setSeed(3L);

        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(geo);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lat, lon from geo_iter order by lat");

        TemplateV2VO template = template("geo-iter-sql", Map.of("geo_iter", source), transform);

        TemplateV2RunResult result = runner(iteratorGeoRegistry()).run(template);

        Assertions.assertEquals(5, result.getRows().size());
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lat")));
    }

    private static TemplateV2Runner runner(TemplateV2RuntimeRegistry registry) {
        return new TemplateV2Runner(registry);
    }

    private static TemplateV2RuntimeRegistry geoJsonRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new GeoJsonSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }

    private static TemplateV2RuntimeRegistry iteratorGeoRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
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
