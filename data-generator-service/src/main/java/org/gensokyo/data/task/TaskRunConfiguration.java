/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.calcite.runtime.WorkflowRunControl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires workflow run control when the full service stack is present.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Configuration
@ConditionalOnBean(TaskExecutionService.class)
public class TaskRunConfiguration {

    /**
     * @param taskExecutionService execution persistence
     * @param pauseCoordinator     manual pause coordinator
     * @return workflow run control bean
     */
    @Bean
    WorkflowRunControl workflowRunControl(
            TaskExecutionService taskExecutionService,
            WorkflowPauseCoordinator pauseCoordinator) {
        return new TaskWorkflowRunControl(taskExecutionService, pauseCoordinator);
    }
}
