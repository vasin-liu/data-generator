/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.geo.GeoAssetService;
import org.gensokyo.data.model.po.AuditEventPO;
import org.gensokyo.data.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST integration tests for {@link ConsoleGeoAssetController} (GEO-05, GEO-08, GOV-01).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
@SpringBootTest(properties = {
        "spring.config.location=classpath:/application-phase7-test.yaml",
        "data.generator.geo-assets.max-bytes=52428800",
        "data.generator.geo-assets.max-features=10000"
})
class ConsoleGeoAssetControllerIT {

    private static final String VALID_FEATURE_COLLECTION = """
            {
              "type": "FeatureCollection",
              "features": [{
                "type": "Feature",
                "geometry": {
                  "type": "Polygon",
                  "coordinates": [[[113.0, 23.0], [113.1, 23.0], [113.1, 23.1], [113.0, 23.1], [113.0, 23.0]]]
                },
                "properties": {"district": "demo"}
              }]
            }
            """;

    private static final String GEOMETRY_ONLY = """
            {"type":"Point","coordinates":[113.3,23.1]}
            """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void upload_validFeatureCollection_returnsSummaryWithId() throws Exception {
        MockMultipartFile file = geoJsonFile(VALID_FEATURE_COLLECTION, "district.geojson");

        mockMvc.perform(multipart("/api/console/geo-assets")
                        .file(file)
                        .param("name", "district-boundary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("district-boundary"))
                .andExpect(jsonPath("$.data.featureCount").value(1))
                .andExpect(jsonPath("$.data.minLon").exists())
                .andExpect(jsonPath("$.data.maxLat").exists());
    }

    @Test
    void upload_createsAuditRecord() throws Exception {
        MockMultipartFile file = geoJsonFile(VALID_FEATURE_COLLECTION, "audit.geojson");

        mockMvc.perform(multipart("/api/console/geo-assets")
                        .file(file)
                        .param("name", "audit-asset"))
                .andExpect(status().isOk());

        List<AuditEventPO> events = auditEventRepository.findByActionOrderByOccurredAtDesc(
                GeoAssetService.AUDIT_ACTION_UPLOAD,
                org.springframework.data.domain.PageRequest.of(0, 5)).getContent();
        org.junit.jupiter.api.Assertions.assertFalse(events.isEmpty());
        AuditEventPO latest = events.getFirst();
        org.junit.jupiter.api.Assertions.assertEquals(GeoAssetService.AUDIT_RESOURCE_TYPE, latest.getResourceType());
        org.junit.jupiter.api.Assertions.assertNotNull(latest.getResourceId());
    }

    @Test
    void list_returnsSummariesWithoutGeoJsonBody() throws Exception {
        MockMultipartFile file = geoJsonFile(VALID_FEATURE_COLLECTION, "list.geojson");
        mockMvc.perform(multipart("/api/console/geo-assets").file(file).param("name", "list-asset"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/console/geo-assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].featureCount").exists())
                .andExpect(jsonPath("$.data[0].geojson").doesNotExist());
    }

    @Test
    void getById_returnsMetadataOnly() throws Exception {
        MockMultipartFile file = geoJsonFile(VALID_FEATURE_COLLECTION, "meta.geojson");
        String id = mockMvc.perform(multipart("/api/console/geo-assets")
                        .file(file)
                        .param("name", "meta-asset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/console/geo-assets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("meta-asset"))
                .andExpect(jsonPath("$.data.featureCount").value(1))
                .andExpect(jsonPath("$.data.geometrySummary").exists());
    }

    @Test
    void getGeoJson_returnsRawApplicationGeoJsonBody() throws Exception {
        MockMultipartFile file = geoJsonFile(VALID_FEATURE_COLLECTION, "body.geojson");
        String id = mockMvc.perform(multipart("/api/console/geo-assets")
                        .file(file)
                        .param("name", "body-asset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/console/geo-assets/" + id + "/geojson"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/geo+json"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FeatureCollection")));
    }

    @Test
    void upload_geometryOnlyRoot_returnsBadRequest() throws Exception {
        MockMultipartFile file = geoJsonFile(GEOMETRY_ONLY, "bad.geojson");

        mockMvc.perform(multipart("/api/console/geo-assets").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private static MockMultipartFile geoJsonFile(String body, String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/geo+json",
                body.getBytes(StandardCharsets.UTF_8));
    }
}
