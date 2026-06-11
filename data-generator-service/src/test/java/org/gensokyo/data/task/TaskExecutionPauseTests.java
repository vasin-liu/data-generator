/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.model.po.TaskExecutionPO;
import org.gensokyo.data.repository.TaskExecutionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for pause reason persistence on task executions.
 *
 * @author Gensokyo
 * @since 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
class TaskExecutionPauseTests {

    private static final Long INSTANCE_ID = 42L;

    @Mock
    private TaskExecutionRepository repository;

    @InjectMocks
    private TaskExecutionService taskExecutionService;

    @AfterEach
    void reset() {
        // MockitoExtension handles mocks per test
    }

    /**
     * Manual workflow pause stores a trimmed operator-visible reason on the row.
     */
    @Test
    void markPausedPersistsPauseReason() {
        TaskExecutionPO row = new TaskExecutionPO();
        row.setInstanceId(INSTANCE_ID);
        row.setStatus(TaskExecutionStatus.RUNNING.name());
        when(repository.findByInstanceId(INSTANCE_ID)).thenReturn(Optional.of(row));
        when(repository.saveAndFlush(any(TaskExecutionPO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskExecutionService.markPaused(INSTANCE_ID, "Manual pause at step pause-gate");

        ArgumentCaptor<TaskExecutionPO> captor = ArgumentCaptor.forClass(TaskExecutionPO.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskExecutionStatus.PAUSED.name());
        assertThat(captor.getValue().getPauseReason()).isEqualTo("Manual pause at step pause-gate");
    }

    /**
     * Resuming clears pause reason when status returns to RUNNING.
     */
    @Test
    void markRunningClearsPauseReason() {
        TaskExecutionPO row = new TaskExecutionPO();
        row.setInstanceId(INSTANCE_ID);
        row.setStatus(TaskExecutionStatus.PAUSED.name());
        row.setPauseReason("Manual pause at step pause-gate");
        when(repository.findByInstanceId(INSTANCE_ID)).thenReturn(Optional.of(row));
        when(repository.saveAndFlush(any(TaskExecutionPO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskExecutionService.markRunning(INSTANCE_ID);

        ArgumentCaptor<TaskExecutionPO> captor = ArgumentCaptor.forClass(TaskExecutionPO.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskExecutionStatus.RUNNING.name());
        assertThat(captor.getValue().getPauseReason()).isNull();
    }
}
