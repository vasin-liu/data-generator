/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.config.DistributedExecutionProperties;
import org.gensokyo.data.config.TaskScheduleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ConsoleRuntimeController}.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@ExtendWith(MockitoExtension.class)
class ConsoleRuntimeControllerTest {

    @Mock
    private DataGeneratorProperties properties;

    @Mock
    private TaskScheduleProperties scheduleProperties;

    @Mock
    private DistributedExecutionProperties distributedProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsoleRuntimeController(
                        properties, scheduleProperties, distributedProperties, null, null, null))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void runtime_returnsFeatureFlags() throws Exception {
        when(properties.isV1ExecutionEnabled()).thenReturn(false);
        when(scheduleProperties.isEnabled()).thenReturn(true);
        when(distributedProperties.isEnabled()).thenReturn(false);
        mockMvc.perform(get("/api/console/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.v1ExecutionEnabled").value(false))
                .andExpect(jsonPath("$.data.scheduleEnabled").value(true))
                .andExpect(jsonPath("$.data.distributedEnabled").value(false));
    }

    @Test
    void editorDataSources_returnsEmptyListsWhenRegistriesAbsent() throws Exception {
        mockMvc.perform(get("/api/console/editor-data-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jdbcNames").isArray())
                .andExpect(jsonPath("$.data.kafkaClusters").isArray())
                .andExpect(jsonPath("$.data.elasticsearchClusters").isArray());
    }
}
