/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.task.DistributedJobLease;
import org.gensokyo.data.task.DistributedJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@link DistributedJobController} worker-facing REST endpoints.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@ExtendWith(MockitoExtension.class)
class DistributedJobControllerTest {

    private static final Long JOB_ID = 88001L;
    private static final Long INSTANCE_ID = 88001001L;
    private static final Long TEMPLATE_ID = 88002L;
    private static final Long TASK_EXECUTION_ID = 88003L;
    private static final String WORKER_ID = "worker-test";

    @Mock
    private DistributedJobService distributedJobService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DistributedJobController(distributedJobService)).build();
    }

    @Test
    void enqueue_returnsJobId() throws Exception {
        when(distributedJobService.enqueue(TASK_EXECUTION_ID, TEMPLATE_ID, INSTANCE_ID, "{\"k\":\"v\"}"))
                .thenReturn(JOB_ID);

        mockMvc.perform(post("/task/distributed/jobs/enqueue/{instanceId}", INSTANCE_ID)
                        .param("taskExecutionId", String.valueOf(TASK_EXECUTION_ID))
                        .param("templateId", String.valueOf(TEMPLATE_ID))
                        .param("payloadJson", "{\"k\":\"v\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(JOB_ID));

        verify(distributedJobService).enqueue(TASK_EXECUTION_ID, TEMPLATE_ID, INSTANCE_ID, "{\"k\":\"v\"}");
    }

    @Test
    void leaseNext_returnsLeasePayload() throws Exception {
        Instant leaseUntil = Instant.parse("2026-06-01T12:00:00Z");
        DistributedJobLease lease = new DistributedJobLease(
                JOB_ID, INSTANCE_ID, TEMPLATE_ID, leaseUntil, "{\"phase\":\"c2\"}");
        when(distributedJobService.leaseNext(WORKER_ID, 30)).thenReturn(Optional.of(lease));

        mockMvc.perform(post("/task/distributed/jobs/lease/next")
                        .param("workerId", WORKER_ID)
                        .param("leaseSeconds", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(JOB_ID))
                .andExpect(jsonPath("$.data.instanceId").value(INSTANCE_ID))
                .andExpect(jsonPath("$.data.templateId").value(TEMPLATE_ID))
                .andExpect(jsonPath("$.data.payloadJson").value("{\"phase\":\"c2\"}"));

        verify(distributedJobService).leaseNext(WORKER_ID, 30);
    }

    @Test
    void leaseNext_returnsNullWhenQueueEmpty() throws Exception {
        when(distributedJobService.leaseNext(WORKER_ID, 30)).thenReturn(Optional.empty());

        mockMvc.perform(post("/task/distributed/jobs/lease/next")
                        .param("workerId", WORKER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void heartbeat_returnsAcknowledgement() throws Exception {
        mockMvc.perform(post("/task/distributed/jobs/{jobId}/heartbeat", JOB_ID)
                        .param("workerId", WORKER_ID)
                        .param("leaseSeconds", "45"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("heartbeat accepted"));

        verify(distributedJobService).heartbeat(JOB_ID, WORKER_ID, 45);
    }

    @Test
    void markRunning_returnsAcknowledgement() throws Exception {
        mockMvc.perform(post("/task/distributed/jobs/{jobId}/running", JOB_ID)
                        .param("workerId", WORKER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("running"));

        verify(distributedJobService).markRunning(JOB_ID, WORKER_ID);
    }

    @Test
    void markSuccess_returnsAcknowledgement() throws Exception {
        mockMvc.perform(post("/task/distributed/jobs/{jobId}/success", JOB_ID)
                        .param("workerId", WORKER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("success"));

        verify(distributedJobService).markSuccess(JOB_ID, WORKER_ID);
    }

    @Test
    void markCancelled_returnsAcknowledgement() throws Exception {
        mockMvc.perform(post("/task/distributed/jobs/{jobId}/cancelled", JOB_ID)
                        .param("workerId", WORKER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("cancelled"));

        verify(distributedJobService).markCancelled(eq(JOB_ID), eq(WORKER_ID));
    }

    @Test
    void markFailed_returnsAcknowledgement() throws Exception {
        mockMvc.perform(post("/task/distributed/jobs/{jobId}/failed", JOB_ID)
                        .param("workerId", WORKER_ID)
                        .param("errorMessage", "boom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("failed"));

        verify(distributedJobService).markFailed(JOB_ID, WORKER_ID, "boom");
    }
}
