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
import org.gensokyo.data.calcite.source.GeoJsonSourceFactory;
import org.gensokyo.data.calcite.source.GeoSyntheticSourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.geo.GeoAssetResolver;
import org.gensokyo.data.geo.io.GeoResourceResolver;
import org.gensokyo.data.model.v2.GeoJsonSourceVO;
import org.gensokyo.data.model.v2.GeoSyntheticSourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Template V2 pipeline proof for {@code asset:{uuid}} / dedicated asset-id binding (GEO-10 / GEO-11).
 *
 * <p>Uses an in-memory {@link GeoAssetResolver} seeded from the classpath Nansha boundary fixture —
 * same execute-path spine as {@code GeoAssetService} without pulling the service module into calcite tests.</p>
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
class TemplateV2RunnerGeoAssetSourceTests {

    private static final String BOUNDARY_FIXTURE = "classpath:geo/南沙区边界.geojson";
    private static final UUID KNOWN_ASSET_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private GeoAssetResolver resolver;
    private TemplateV2RuntimeRegistry registry;

    @BeforeEach
    void setUp() {
        String geoJson;
        try {
            geoJson = GeoResourceResolver.readUtf8(BOUNDARY_FIXTURE);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load boundary fixture for asset pipeline IT", ex);
        }
        Map<String, String> assets = new LinkedHashMap<>();
        assets.put(KNOWN_ASSET_ID.toString(), geoJson);
        resolver = assetId -> {
            String body = assets.get(assetId);
            if (body == null) {
                throw new IllegalArgumentException("Unknown geo asset id: " + assetId);
            }
            return body;
        };
        registry = new TemplateV2RuntimeRegistry(
                List.of(new GeoSyntheticSourceFactory(resolver), new GeoJsonSourceFactory(resolver)),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }

    @Test
    void boundaryPoints_withBoundaryAssetId_returnsExpectedRowCount() {
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BOUNDARY_POINTS");
        source.setBoundaryAssetId(KNOWN_ASSET_ID.toString());
        source.setCount(6);
        source.setSeed(11L);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lon, lat from geo_boundary");

        TemplateV2VO template = template("geo-asset-boundary", Map.of("geo_boundary", source), transform);

        TemplateV2RunResult result = new TemplateV2Runner(registry).run(template);

        Assertions.assertEquals(6, result.getRows().size());
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lat")));
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lon")));
    }

    @Test
    void geojson_withAssetId_returnsNonEmptyLonLatRows() {
        GeoJsonSourceVO source = new GeoJsonSourceVO();
        source.setAssetId(KNOWN_ASSET_ID.toString());

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lon, lat from geo_in");

        TemplateV2VO template = template("geo-asset-geojson", Map.of("geo_in", source), transform);

        TemplateV2RunResult result = new TemplateV2Runner(registry).run(template);

        Assertions.assertFalse(result.getRows().isEmpty());
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lat")));
        Assertions.assertTrue(result.getRows().stream().allMatch(row -> row.values().containsKey("lon")));
    }

    @Test
    void boundaryPoints_withAssetWireFormatPath_matchesDedicatedField() {
        GeoSyntheticSourceVO dedicated = new GeoSyntheticSourceVO();
        dedicated.setMode("BOUNDARY_POINTS");
        dedicated.setBoundaryAssetId(KNOWN_ASSET_ID.toString());
        dedicated.setCount(5);
        dedicated.setSeed(99L);

        GeoSyntheticSourceVO wire = new GeoSyntheticSourceVO();
        wire.setMode("BOUNDARY_POINTS");
        wire.setBoundaryPath("asset:" + KNOWN_ASSET_ID);
        wire.setCount(5);
        wire.setSeed(99L);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lon, lat from geo_boundary");

        TemplateV2RunResult dedicatedResult = new TemplateV2Runner(registry).run(
                template("dedicated", Map.of("geo_boundary", dedicated), transform));
        TemplateV2RunResult wireResult = new TemplateV2Runner(registry).run(
                template("wire", Map.of("geo_boundary", wire), transform));

        Assertions.assertEquals(dedicatedResult.getRows().size(), wireResult.getRows().size());
        Assertions.assertEquals(5, wireResult.getRows().size());
    }

    @Test
    void unknownAssetId_failsWithIllegalArgumentNamingId() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000099");
        GeoSyntheticSourceVO source = new GeoSyntheticSourceVO();
        source.setMode("BOUNDARY_POINTS");
        source.setBoundaryAssetId(unknown.toString());
        source.setCount(3);
        source.setSeed(1L);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("select lon, lat from geo_boundary");

        TemplateV2VO template = template("unknown-asset", Map.of("geo_boundary", source), transform);

        // Registry wraps source-factory IAE as IllegalStateException; root cause must name the asset id (D-09).
        RuntimeException ex = Assertions.assertThrows(
                RuntimeException.class,
                () -> new TemplateV2Runner(registry).run(template));
        Assertions.assertTrue(causeChainContainsAssetId(ex, unknown.toString()), ex.toString());
    }

    private static boolean causeChainContainsAssetId(Throwable error, String assetId) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof IllegalArgumentException
                    && current.getMessage() != null
                    && current.getMessage().contains(assetId)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
