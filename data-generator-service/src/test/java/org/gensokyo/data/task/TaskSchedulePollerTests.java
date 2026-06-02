/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.config.TaskScheduleProperties;
import org.gensokyo.data.controller.TaskController;
import org.gensokyo.data.model.po.TaskSchedulePO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskSchedulePoller}.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@ExtendWith(MockitoExtension.class)
class TaskSchedulePollerTests {

    @Mock
    private TaskScheduleProperties taskScheduleProperties;

    @Mock
    private TaskScheduleService taskScheduleService;

    @Mock
    private TaskController taskController;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TaskSchedulePoller taskSchedulePoller;

    @Test
    void pollDueTriggersScheduledRunAndAdvancesSchedule() {
        when(taskScheduleProperties.isEnabled()).thenReturn(true);

        TaskSchedulePO due = new TaskSchedulePO();
        due.setId(501L);
        due.setTemplateId(99001L);
        due.setCronExpression("0 * * * * *");
        when(taskScheduleService.findDue(any())).thenReturn(List.of(due));
        when(taskController.triggerScheduledRun(99001L))
                .thenReturn(new TaskController.TemplateRunStartResult(99001L, "demo", 88001L));

        taskSchedulePoller.pollDueSchedules();

        verify(taskController).triggerScheduledRun(99001L);
        verify(taskScheduleService).markTriggered(eq(501L), any(Instant.class), eq(88001L));
        verify(auditService).record(eq("TASK_SCHEDULE_TRIGGER"), eq("TASK_SCHEDULE"), eq("501"), any());
    }
}
