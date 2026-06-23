/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.udf.TransformCatalogSource;
import org.gensokyo.data.udf.UdfLifecycleState;
import org.gensokyo.data.udf.UdfRecord;
import org.gensokyo.data.udf.UdfRegistryService;
import org.gensokyo.data.udf.UdfType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc tests for {@link ConsoleTransformCatalogController}: unified catalog of built-in
 * operators + published UDFs, excluding non-published UDFs and internal scalar functions.
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
@ExtendWith(MockitoExtension.class)
class ConsoleTransformCatalogControllerTest {

    @Mock
    private UdfRegistryService udfRegistryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConsoleTransformCatalogController(new TransformCatalogSource(udfRegistryService)))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void list_returnsBuiltinsAndPublishedUdfOnly() throws Exception {
        UdfRecord published = sqlRecord("com.acme.greet", "1.0.0", "GREET", UdfLifecycleState.PUBLISHED);
        UdfRecord draft = sqlRecord("com.acme.ghost", "1.0.0", "GHOST", UdfLifecycleState.DRAFT);
        when(udfRegistryService.list(any())).thenReturn(List.of(published, draft));

        mockMvc.perform(get("/api/console/transforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // Built-in operators present with rich metadata (D-07).
                .andExpect(jsonPath("$.data[?(@.type == 'json' && @.kind == 'BUILTIN')].params").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type == 'json')].example").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type == 'mask' && @.kind == 'BUILTIN')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type == 'lookup' && @.kind == 'BUILTIN')]").isNotEmpty())
                // Published UDF folded in and tagged UDF (D-06).
                .andExpect(jsonPath("$.data[?(@.type == 'com.acme.greet' && @.kind == 'UDF')]").isNotEmpty())
                // Non-published UDF excluded.
                .andExpect(jsonPath("$.data[?(@.type == 'com.acme.ghost')]").isEmpty())
                // Internal scalar function never cataloged (D-12).
                .andExpect(jsonPath("$.data[?(@.type == 'V2_JSON_EXTRACT')]").isEmpty());
    }

    @Test
    void list_kindFilterReturnsBuiltinsOnly() throws Exception {
        UdfRecord published = sqlRecord("com.acme.greet", "1.0.0", "GREET", UdfLifecycleState.PUBLISHED);
        when(udfRegistryService.list(any())).thenReturn(List.of(published));

        mockMvc.perform(get("/api/console/transforms").param("kind", "BUILTIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.type == 'json')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.kind == 'UDF')]").isEmpty());
    }

    @Test
    void list_kindFilterReturnsUdfsOnly() throws Exception {
        UdfRecord published = sqlRecord("com.acme.greet", "1.0.0", "GREET", UdfLifecycleState.PUBLISHED);
        when(udfRegistryService.list(any())).thenReturn(List.of(published));

        mockMvc.perform(get("/api/console/transforms").param("kind", "UDF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.kind == 'UDF')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.kind == 'BUILTIN')]").isEmpty());
    }

    @Test
    void list_builtinOperatorsHaveCompleteMetadata() throws Exception {
        when(udfRegistryService.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/console/transforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type == 'json')].params[0].name").value("sourceColumn"))
                .andExpect(jsonPath("$.data[?(@.type == 'json')].example").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type == 'mask')].params").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type == 'mask')].example").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type == 'lookup')].params").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type == 'lookup')].example").isNotEmpty());
    }

    @Test
    void list_unknownKind_mapsToBadRequest() throws Exception {
        mockMvc.perform(get("/api/console/transforms").param("kind", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private static UdfRecord sqlRecord(String udfId, String version, String sqlName, UdfLifecycleState state) {
        String payload = "{\"sqlName\":\"" + sqlName + "\",\"argCount\":1,\"returnType\":\"VARCHAR\","
                + "\"script\":\"return args[0];\"}";
        UdfRecord.Builder builder = new UdfRecord.Builder()
                .udfId(udfId)
                .version(version)
                .type(UdfType.SQL)
                .state(state)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .metadata(Map.of())
                .registeredAt(Instant.parse("2026-06-22T00:00:00Z"));
        if (state == UdfLifecycleState.PUBLISHED) {
            builder.publishedAt(Instant.parse("2026-06-22T01:00:00Z"));
        }
        return builder.build();
    }
}
