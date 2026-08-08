/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST IT for console geo preview helpers (GEO-12 / D-06 / D-08).
 *
 * @author Gensokyo
 * @since 2026-08-06
 */
@SpringBootTest(properties = {
        "spring.config.location=classpath:/application-phase7-test.yaml",
        "data.generator.geo-assets.max-bytes=52428800",
        "data.generator.geo-assets.max-features=10000"
})
class ConsoleGeoAssetPreviewIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void previewLocation_classpathFixture_returnsApplicationGeoJson() throws Exception {
        String body = """
                {"location":"classpath:geo/preview-point.geojson"}
                """;
        mockMvc.perform(post("/api/console/geo-assets/preview/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/geo+json"))
                .andExpect(content().string(Matchers.containsString("FeatureCollection")))
                .andExpect(content().string(Matchers.containsString("preview-point")));
    }

    @Test
    void previewSynthetic_bboxMaxCount5_returnsSeedAndAtMostFivePoints() throws Exception {
        String body = """
                {
                  "mode": "BBOX",
                  "seed": 11,
                  "maxCount": 5,
                  "bbox": [113.0, 23.0, 113.2, 23.2]
                }
                """;
        mockMvc.perform(post("/api/console/geo-assets/preview/synthetic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seed").value(11))
                .andExpect(jsonPath("$.data.effectiveSampleCount").value(Matchers.lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.data.featureCollection.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.data.featureCollection.features.length()")
                        .value(Matchers.lessThanOrEqualTo(5)));
    }

    @Test
    void previewSynthetic_lineSampleByCount_returnsFeatures() throws Exception {
        String body = """
                {
                  "mode": "LINE_SAMPLE",
                  "seed": 21,
                  "maxCount": 6,
                  "networkPath": "classpath:geo/preview-line.geojson",
                  "sample": { "strategy": "BY_COUNT" }
                }
                """;
        mockMvc.perform(post("/api/console/geo-assets/preview/synthetic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seed").value(21))
                .andExpect(jsonPath("$.data.effectiveSampleCount").value(6))
                .andExpect(jsonPath("$.data.featureCollection.features.length()").value(6));
    }

    @Test
    void previewSynthetic_lineSampleBySpacing_returnsFewerThanMaxCount() throws Exception {
        String body = """
                {
                  "mode": "LINE_SAMPLE",
                  "seed": 22,
                  "maxCount": 40,
                  "networkPath": "classpath:geo/preview-line.geojson",
                  "sample": { "strategy": "BY_SPACING_METERS", "spacingMeters": 500 }
                }
                """;
        mockMvc.perform(post("/api/console/geo-assets/preview/synthetic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.effectiveSampleCount").value(Matchers.lessThan(40)))
                .andExpect(jsonPath("$.data.featureCollection.features.length()")
                        .value(Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void previewSynthetic_maxCount501_returnsBadRequest() throws Exception {
        String body = """
                {
                  "mode": "BBOX",
                  "seed": 1,
                  "maxCount": 501,
                  "bbox": [113.0, 23.0, 113.2, 23.2]
                }
                """;
        mockMvc.perform(post("/api/console/geo-assets/preview/synthetic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("500")));
    }
}
