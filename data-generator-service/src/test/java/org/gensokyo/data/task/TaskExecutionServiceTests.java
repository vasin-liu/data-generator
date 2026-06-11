/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TaskExecutionPO;
import org.gensokyo.data.repository.TaskExecutionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TaskExecutionService} lifecycle persistence.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TaskExecutionServiceTests {

    private static final long TEMPLATE_ID = 9001L;
    private static final long INSTANCE_ID = 90010001L;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskExecutionRepository repository;

    @AfterEach
    void cleanup() {
        repository.findByInstanceId(INSTANCE_ID).ifPresent(repository::delete);
        repository.findByInstanceId(99001L).ifPresent(repository::delete);
    }

    @Test
    void persistsPipelineReservationFields() {
        TaskExecutionPO po = new TaskExecutionPO();
        po.setId(99L);
        po.setTemplateId(1L);
        po.setInstanceId(99001L);
        po.setStatus("SUCCEEDED");
        po.setParentPipelineRunId("pipe-run-1");
        po.setUpstreamArtifactRefsJson("[{\"nodeId\":\"a\",\"artifactId\":\"art-1\"}]");
        repository.save(po);
        TaskExecutionPO loaded = repository.findById(99L).orElseThrow();
        assertThat(loaded.getParentPipelineRunId()).isEqualTo("pipe-run-1");
        assertThat(loaded.getUpstreamArtifactRefsJson()).contains("art-1");
    }

    @Test
    void lifecyclePersistsTerminalSuccess() {
        taskExecutionService.queueExecution(TEMPLATE_ID, "test-template", INSTANCE_ID, "V2");
        Assertions.assertTrue(taskExecutionService.isRunning(TEMPLATE_ID));
        taskExecutionService.markRunning(INSTANCE_ID);
        taskExecutionService.markSuccess(INSTANCE_ID, 42L, "{\"mode\":\"test\"}");
        Assertions.assertFalse(taskExecutionService.isRunning(TEMPLATE_ID));
        TaskExecutionSummary summary = taskExecutionService.getByInstanceId(INSTANCE_ID);
        Assertions.assertEquals(TaskExecutionStatus.SUCCESS.name(), summary.status());
        Assertions.assertEquals(42L, summary.rowCount());
    }
}
