/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.DataGeneratorApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
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
}
