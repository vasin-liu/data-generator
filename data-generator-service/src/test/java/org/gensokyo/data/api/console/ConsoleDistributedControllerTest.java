/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.api.console.dto.DistributedQueueMetricsDto;
import org.gensokyo.data.api.console.dto.WorkerHealthDto;
import org.gensokyo.data.task.DistributedJobMetricsService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@link ConsoleDistributedController}.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@ExtendWith(MockitoExtension.class)
class ConsoleDistributedControllerTest {

    @Mock
    private DistributedJobMetricsService distributedJobMetricsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsoleDistributedController(distributedJobMetricsService))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void metrics_returnsQueueSnapshot() throws Exception {
        Instant collectedAt = Instant.parse("2026-06-01T12:00:00Z");
        when(distributedJobMetricsService.queueMetrics())
                .thenReturn(new DistributedQueueMetricsDto(
                        true,
                        false,
                        true,
                        Map.of("QUEUED", 2L, "RUNNING", 1L),
                        List.of(new WorkerHealthDto("worker-a", 1L)),
                        collectedAt));

        mockMvc.perform(get("/api/console/distributed/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.distributedEnabled").value(true))
                .andExpect(jsonPath("$.data.jobsByStatus.QUEUED").value(2))
                .andExpect(jsonPath("$.data.activeWorkers[0].workerId").value("worker-a"));
    }
}
