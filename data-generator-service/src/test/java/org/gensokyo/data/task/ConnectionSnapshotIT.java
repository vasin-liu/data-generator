/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.snapshot.ExecutionConnectionSnapshot;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TaskExecutionPO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.repository.TaskExecutionRepository;
import org.gensokyo.data.util.RandomKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for RUNNING-time connection snapshot persistence (D-02, D-04, D-07).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class ConnectionSnapshotIT {

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private ConnectionCatalog connectionCatalog;

    @Test
    @Transactional
    void queuedExecutionHasNoSnapshotUntilRunning() {
        Long instanceId = RandomKit.snowFlake().nextId();
        taskExecutionService.queueExecution(1L, "snap-test", instanceId, "V2");

        TaskExecutionPO queued = taskExecutionRepository.findByInstanceId(instanceId).orElseThrow();
        Assertions.assertEquals(TaskExecutionStatus.QUEUED.name(), queued.getStatus());
        Assertions.assertTrue(
                queued.getConnectionSnapshotJson() == null || queued.getConnectionSnapshotJson().isBlank());

        TemplateV2VO template = templateWithJdbcSource("data-generator");
        taskExecutionService.markRunning(instanceId);
        taskExecutionService.captureConnectionSnapshot(instanceId, template, connectionCatalog);

        TaskExecutionPO running = taskExecutionRepository.findByInstanceId(instanceId).orElseThrow();
        Assertions.assertEquals(TaskExecutionStatus.RUNNING.name(), running.getStatus());
        Assertions.assertNotNull(running.getConnectionSnapshotJson());
        ExecutionConnectionSnapshot snapshot =
                TemplateJsonCodec.read(running.getConnectionSnapshotJson(), ExecutionConnectionSnapshot.class);
        Assertions.assertFalse(snapshot.connections().isEmpty());
        Assertions.assertTrue(snapshot.connections().stream()
                .anyMatch(ref -> ref.kind() == ConnectionKind.JDBC && "data-generator".equals(ref.name())));
    }

    @Test
    @Transactional
    void workerPathResolvesSnapshotFromDatabaseJsonOnly() {
        Long instanceId = RandomKit.snowFlake().nextId();
        taskExecutionService.queueExecution(2L, "worker-snap", instanceId, "V2");
        TemplateV2VO template = templateWithJdbcSource("compare-inline-ds");
        taskExecutionService.markRunning(instanceId);
        taskExecutionService.captureConnectionSnapshot(instanceId, template, connectionCatalog);

        String persistedJson = taskExecutionRepository.findByInstanceId(instanceId)
                .orElseThrow()
                .getConnectionSnapshotJson();
        Assertions.assertNotNull(persistedJson);

        // Simulate worker JVM with empty in-process cache — load from DB JSON only (D-07).
        TaskExecutionService workerView = taskExecutionService;
        ExecutionConnectionSnapshot loaded = workerView.getConnectionSnapshot(instanceId).orElseThrow();
        Assertions.assertEquals(
                TemplateJsonCodec.read(persistedJson, ExecutionConnectionSnapshot.class).capturedAt(),
                loaded.capturedAt());
        Assertions.assertTrue(loaded.connections().stream()
                .anyMatch(ref -> "compare-inline-ds".equals(ref.name())));
    }

    private static TemplateV2VO templateWithJdbcSource(String dataSourceId) {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("snap-template");
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId(dataSourceId);
        template.getSources().put("src", source);
        return template;
    }
}
