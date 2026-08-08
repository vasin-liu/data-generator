/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.api.console.dto.GeoAssetSummaryView;
import org.gensokyo.data.api.console.dto.GeoPreviewLocationRequest;
import org.gensokyo.data.api.console.dto.GeoSyntheticPreviewRequest;
import org.gensokyo.data.api.console.dto.GeoSyntheticPreviewView;
import org.gensokyo.data.model.po.GeoAssetPO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service-layer tests for geo asset ingest, limits, and resolution (GEO-05).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class GeoAssetServiceTests {

    private static final String VALID_FEATURE_COLLECTION = """
            {
              "type": "FeatureCollection",
              "features": [{
                "type": "Feature",
                "geometry": {
                  "type": "Point",
                  "coordinates": [113.3, 23.1]
                },
                "properties": {"name": "test-point"}
              }]
            }
            """;

    @Autowired
    private GeoAssetService geoAssetService;

    @Test
    @Transactional
    void upload_validFeatureCollection_persistsAndResolves() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "boundary.geojson",
                "application/geo+json",
                VALID_FEATURE_COLLECTION.getBytes(StandardCharsets.UTF_8));

        var upload = geoAssetService.upload(file, "test-boundary", "test-actor");
        Assertions.assertNotNull(upload.id());
        Assertions.assertEquals("test-boundary", upload.name());
        Assertions.assertEquals(1, upload.featureCount());

        String body = geoAssetService.resolveUtf8(upload.id().toString());
        Assertions.assertTrue(body.contains("FeatureCollection"));
        Assertions.assertEquals(body, geoAssetService.getGeoJsonBody(upload.id()));
    }

    @Test
    void resolveUtf8_unknownId_throwsWithIdInMessage() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000001");
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> geoAssetService.resolveUtf8(unknown.toString()));
        Assertions.assertTrue(ex.getMessage().contains(unknown.toString()));
    }

    @Test
    void ingest_exceedsMaxBytes_rejectsBeforePersist() {
        byte[] bytes = VALID_FEATURE_COLLECTION.getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeoAssetIngestSupport.ingest(bytes, 10, 10_000));
        Assertions.assertTrue(ex.getMessage().contains("max size"));
    }

    @Test
    void ingest_exceedsMaxFeatures_rejectsBeforePersist() {
        byte[] bytes = VALID_FEATURE_COLLECTION.getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeoAssetIngestSupport.ingest(bytes, 1_048_576, 0));
        Assertions.assertTrue(ex.getMessage().contains("feature count"));
    }

    @Test
    void ingest_geometryOnlyRoot_rejects() {
        String geometryOnly = """
                {"type":"Point","coordinates":[113.3,23.1]}
                """;
        byte[] bytes = geometryOnly.getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> GeoAssetIngestSupport.ingest(bytes, 1_048_576, 10_000));
        Assertions.assertTrue(ex.getMessage().contains("Feature") || ex.getMessage().contains("FeatureCollection"));
    }

    @Test
    void previewLocation_classpathFixture_returnsFeatureCollectionUtf8() throws Exception {
        String body = geoAssetService.previewLocation(
                new GeoPreviewLocationRequest("classpath:geo/preview-point.geojson"));
        Assertions.assertTrue(body.contains("FeatureCollection"));
        Assertions.assertTrue(body.contains("preview-point"));
    }

    @Test
    void previewLocation_blankLocation_throwsIllegalArgumentException() {
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> geoAssetService.previewLocation(new GeoPreviewLocationRequest("  ")));
        Assertions.assertTrue(ex.getMessage().toLowerCase().contains("blank")
                || ex.getMessage().toLowerCase().contains("location"));
    }

    @Test
    void previewSynthetic_maxCountOverCap_throwsNamingCap() {
        GeoSyntheticPreviewRequest request = new GeoSyntheticPreviewRequest(
                "BBOX",
                42L,
                501,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(113.0, 23.0, 113.2, 23.2),
                null,
                null,
                null,
                null);
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> geoAssetService.previewSynthetic(request));
        Assertions.assertTrue(
                ex.getMessage().contains("500") || ex.getMessage().contains(String.valueOf(GeoAssetService.PREVIEW_MAX_COUNT)),
                () -> "expected cap in message: " + ex.getMessage());
    }

    @Test
    void previewSynthetic_bboxMaxCount10_returnsAtMost10PointsAndSeed() throws Exception {
        GeoSyntheticPreviewRequest request = new GeoSyntheticPreviewRequest(
                "BBOX",
                7L,
                10,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(113.0, 23.0, 113.2, 23.2),
                null,
                null,
                null,
                null);
        GeoSyntheticPreviewView view = geoAssetService.previewSynthetic(request);
        Assertions.assertEquals(7L, view.seed());
        Assertions.assertTrue(view.effectiveSampleCount() <= 10);
        Assertions.assertEquals(GeoAssetService.PREVIEW_MAX_COUNT, view.maxCountCap());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) view.featureCollection().get("features");
        Assertions.assertNotNull(features);
        Assertions.assertTrue(features.size() <= 10);
        Assertions.assertFalse(features.isEmpty());
    }

    @Test
    void previewSynthetic_lineSampleByCount_returnsRequestedPointCount() throws Exception {
        GeoSyntheticPreviewRequest request = new GeoSyntheticPreviewRequest(
                "LINE_SAMPLE",
                3L,
                12,
                null,
                null,
                "classpath:geo/preview-line.geojson",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new GeoSyntheticPreviewRequest.Sample("BY_COUNT", null));
        GeoSyntheticPreviewView view = geoAssetService.previewSynthetic(request);
        Assertions.assertEquals(3L, view.seed());
        Assertions.assertEquals(12, view.effectiveSampleCount());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) view.featureCollection().get("features");
        Assertions.assertEquals(12, features.size());
    }

    @Test
    void previewSynthetic_lineSampleBySpacing_appliesStrategyNotIgnoredAsByCount() throws Exception {
        // ~2 km east-west line; spacing 500 m → floor(L/500)+1 points, far below maxCount 40.
        GeoSyntheticPreviewRequest request = new GeoSyntheticPreviewRequest(
                "LINE_SAMPLE",
                5L,
                40,
                null,
                null,
                "classpath:geo/preview-line.geojson",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new GeoSyntheticPreviewRequest.Sample("BY_SPACING_METERS", 500d));
        GeoSyntheticPreviewView view = geoAssetService.previewSynthetic(request);
        Assertions.assertTrue(view.effectiveSampleCount() < 40,
                () -> "expected spacing strategy to yield fewer than maxCount points, got "
                        + view.effectiveSampleCount());
        Assertions.assertTrue(view.effectiveSampleCount() >= 2);
    }

    @Test
    void previewSynthetic_lineSampleOmittedSample_defaultsByCount() throws Exception {
        GeoSyntheticPreviewRequest request = new GeoSyntheticPreviewRequest(
                "LINE_SAMPLE",
                9L,
                8,
                null,
                null,
                "classpath:geo/preview-line.geojson",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        GeoSyntheticPreviewView view = geoAssetService.previewSynthetic(request);
        Assertions.assertEquals(8, view.effectiveSampleCount());
    }

    @Test
    void summaryView_from_includesContentTypeWhenPresentOnPo() {
        GeoAssetPO row = new GeoAssetPO();
        row.setId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
        row.setName("typed");
        row.setContentType("application/geo+json");
        row.setFeatureCount(1);
        row.setMinLon(0);
        row.setMinLat(0);
        row.setMaxLon(1);
        row.setMaxLat(1);
        row.setUploadedBy("tester");
        Instant now = Instant.parse("2026-08-06T00:00:00Z");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);

        GeoAssetSummaryView view = GeoAssetSummaryView.from(row);
        Assertions.assertEquals("application/geo+json", view.contentType());
    }
}
