/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.api.console.dto.TaskScheduleUpsertRequest;
import org.gensokyo.data.api.console.dto.TaskScheduleView;
import org.gensokyo.data.task.TaskScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@link ConsoleScheduleController}.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@ExtendWith(MockitoExtension.class)
class ConsoleScheduleControllerTest {

    @Mock
    private TaskScheduleService taskScheduleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsoleScheduleController(taskScheduleService))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void list_returnsSchedules() throws Exception {
        Instant next = Instant.parse("2026-06-01T12:00:00Z");
        when(taskScheduleService.listAll())
                .thenReturn(List.of(new TaskScheduleView(1L, 99L, "0 0 * * * *", true, "nightly", null, 123L, next)));

        mockMvc.perform(get("/api/console/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].templateId").value(99));
    }

    @Test
    void create_returnsSchedule() throws Exception {
        Instant next = Instant.parse("2026-06-01T12:00:00Z");
        when(taskScheduleService.create(any(TaskScheduleUpsertRequest.class)))
                .thenReturn(new TaskScheduleView(2L, 100L, "0 0 * * * *", true, null, null, null, next));

        mockMvc.perform(
                        post("/api/console/schedules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"templateId":100,"cronExpression":"0 0 * * * *","enabled":true}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void previewCron_returnsNextTrigger() throws Exception {
        Instant next = Instant.parse("2026-06-01T12:00:00Z");
        when(taskScheduleService.previewNextTrigger("0 0 2 * * *")).thenReturn(next);

        mockMvc.perform(get("/api/console/schedules/preview").param("cron", "0 0 2 * * *"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(next.toString()));
    }
}
