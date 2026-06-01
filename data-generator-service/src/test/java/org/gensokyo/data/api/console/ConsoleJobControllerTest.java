/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.task.TaskExecutionSummary;
import org.gensokyo.data.task.WorkflowPauseCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ConsoleJobController}.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@ExtendWith(MockitoExtension.class)
class ConsoleJobControllerTest {

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private WorkflowPauseCoordinator workflowPauseCoordinator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConsoleJobController(taskExecutionService, workflowPauseCoordinator))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void list_returnsExecutions() throws Exception {
        TaskExecutionSummary row = new TaskExecutionSummary(
                99L, 10L, "demo", 1L, "V2", "SUCCESS", null, null, null, 5L, null, null, null);
        when(taskExecutionService.list(any())).thenReturn(List.of(row));
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].instanceId").value(1));
    }
}
