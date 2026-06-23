/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.udf.UdfLifecycleState;
import org.gensokyo.data.udf.UdfRecord;
import org.gensokyo.data.udf.UdfRegistryException;
import org.gensokyo.data.udf.UdfPublishService;
import org.gensokyo.data.udf.UdfRegistryService;
import org.gensokyo.data.udf.UdfType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc tests for {@link ConsoleUdfController} with mocked Phase 2 services.
 *
 * @author Gensokyo
 * @since 2026-06-18
 */
@ExtendWith(MockitoExtension.class)
class ConsoleUdfControllerTest {

    @Mock
    private UdfRegistryService udfRegistryService;

    @Mock
    private UdfPublishService udfPublishService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConsoleUdfController(udfRegistryService, udfPublishService))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void uploadSql_returnsDraftView() throws Exception {
        UdfRecord draft = record("com.acme.greet", "1.0.0", UdfType.SQL, UdfLifecycleState.DRAFT);
        when(udfRegistryService.registerDraft(
                eq("com.acme.greet"), eq("1.0.0"), eq(UdfType.SQL), any(), any())).thenReturn(draft);

        mockMvc.perform(multipart("/api/console/udfs")
                        .param("udfId", "com.acme.greet")
                        .param("version", "1.0.0")
                        .param("type", "sql")
                        .param("sql", "SELECT 1")
                        .param("sqlName", "GREET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.state").value("draft"))
                .andExpect(jsonPath("$.data.payload").doesNotExist());
    }

    @Test
    void publish_returnsPublishedView() throws Exception {
        UdfRecord published = record("com.acme.greet", "1.0.0", UdfType.SQL, UdfLifecycleState.PUBLISHED);
        when(udfPublishService.publish("com.acme.greet", "1.0.0")).thenReturn(published);

        mockMvc.perform(post("/api/console/udfs/com.acme.greet/1.0.0/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.state").value("published"));
    }

    @Test
    void list_returnsGroupedJsonWithoutPayload() throws Exception {
        UdfRecord v1 = record("com.acme.greet", "1.0.0", UdfType.SCRIPT, UdfLifecycleState.PUBLISHED);
        UdfRecord v2 = record("com.acme.greet", "2.0.0", UdfType.SCRIPT, UdfLifecycleState.DRAFT);
        when(udfRegistryService.list(any())).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/console/udfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].udfId").value("com.acme.greet"))
                .andExpect(jsonPath("$.data[0].versions").isArray())
                .andExpect(jsonPath("$.data[0].versions[0].version").value("1.0.0"))
                .andExpect(jsonPath("$.data[0].versions[0].payload").doesNotExist());
    }

    @Test
    void publish_unknownUdf_mapsToBadRequestWithCode() throws Exception {
        when(udfPublishService.publish("com.acme.ghost", "1.0.0"))
                .thenThrow(new UdfRegistryException("UDF_NOT_FOUND", "No UDF [com.acme.ghost@1.0.0] to publish"));

        mockMvc.perform(post("/api/console/udfs/com.acme.ghost/1.0.0/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("UDF_NOT_FOUND"))
                .andExpect(jsonPath("$.data.violations[0].code").value("UDF_NOT_FOUND"));
    }

    @Test
    void history_returnsVersionTimelineForUdfId() throws Exception {
        UdfRecord v1 = record("com.acme.greet", "1.0.0", UdfType.SQL, UdfLifecycleState.PUBLISHED);
        UdfRecord v2 = record("com.acme.greet", "2.0.0", UdfType.SQL, UdfLifecycleState.DRAFT);
        when(udfRegistryService.list(any())).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/console/udfs/com.acme.greet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.udfId").value("com.acme.greet"))
                .andExpect(jsonPath("$.data.versions.length()").value(2))
                .andExpect(jsonPath("$.data.versions[0].version").value("1.0.0"))
                .andExpect(jsonPath("$.data.versions[0].payload").doesNotExist());
    }

    @Test
    void deprecate_afterPublish_reflectsDeprecatedStateInListing() throws Exception {
        UdfRecord published = record("com.acme.greet", "1.0.0", UdfType.SQL, UdfLifecycleState.PUBLISHED);
        UdfRecord deprecated = record("com.acme.greet", "1.0.0", UdfType.SQL, UdfLifecycleState.DEPRECATED);
        when(udfPublishService.deprecate("com.acme.greet", "1.0.0")).thenReturn(deprecated);
        when(udfRegistryService.list(any())).thenReturn(List.of(deprecated));

        mockMvc.perform(post("/api/console/udfs/com.acme.greet/1.0.0/deprecate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("deprecated"));

        mockMvc.perform(get("/api/console/udfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].versions[0].state").value("deprecated"));
    }

    @Test
    void deprecate_unknownUdf_mapsToBadRequestWithCode() throws Exception {
        when(udfPublishService.deprecate("com.acme.ghost", "1.0.0"))
                .thenThrow(new UdfRegistryException("UDF_NOT_FOUND", "No UDF [com.acme.ghost@1.0.0] to deprecate"));

        mockMvc.perform(post("/api/console/udfs/com.acme.ghost/1.0.0/deprecate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("UDF_NOT_FOUND"));
    }

    private static UdfRecord record(String udfId, String version, UdfType type, UdfLifecycleState state) {
        UdfRecord.Builder builder = new UdfRecord.Builder()
                .udfId(udfId)
                .version(version)
                .type(type)
                .state(state)
                .payload("secret-artifact-bytes".getBytes())
                .metadata(Map.of())
                .registeredAt(Instant.parse("2026-06-18T00:00:00Z"));
        if (state == UdfLifecycleState.PUBLISHED) {
            builder.publishedAt(Instant.parse("2026-06-18T01:00:00Z"));
        }
        return builder.build();
    }
}
