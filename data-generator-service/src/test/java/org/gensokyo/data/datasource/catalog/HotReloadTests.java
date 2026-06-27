/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.runtime.WorkflowRunContext;
import org.gensokyo.data.calcite.runtime.WorkflowRunControl;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionHealthStatus;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.JdbcResolvedConnection;
import org.gensokyo.data.datasource.api.snapshot.ExecutionConnectionSnapshot;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.util.RandomKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Hot-reload isolation and DEGRADED fallback scenarios (D-09, D-10, D-11).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class HotReloadTests {

    private static final String DS_NAME = "hot-reload-it-ds";

    @Autowired
    private ConnectionCatalogImpl connectionCatalogImpl;

    @Autowired
    private ConnectionCatalog connectionCatalog;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Autowired
    private HotReloadCoordinator hotReloadCoordinator;

    @BeforeEach
    void resetDatasourceRow() {
        dataSourceConfigRepository.findById(DS_NAME).ifPresent(dataSourceConfigRepository::delete);
        if (dynamicRoutingDataSource.getDataSources().containsKey(DS_NAME)) {
            dynamicRoutingDataSource.removeDataSource(DS_NAME);
        }
    }

    @Test
    @Transactional
    void inFlightRunKeepsPreReloadSnapshotParamsAfterDatasourceSave() throws Exception {
        String originalUrl = "jdbc:h2:mem:hotreload-original;DB_CLOSE_DELAY=-1";
        persistAndReload(originalUrl);

        Long instanceId = RandomKit.snowFlake().nextId();
        taskExecutionService.queueExecution(10L, "hot-reload-inflight", instanceId, "V2");
        TemplateV2VO template = jdbcTemplate(DS_NAME);
        taskExecutionService.markRunning(instanceId);
        taskExecutionService.captureConnectionSnapshot(instanceId, template, connectionCatalogImpl);

        ExecutionConnectionSnapshot beforeSave = taskExecutionService.getConnectionSnapshot(instanceId).orElseThrow();
        String snapshottedUrl = String.valueOf(beforeSave.connections().getFirst().configParams().get("url"));
        Assertions.assertEquals(originalUrl, snapshottedUrl);

        WorkflowRunContext.bind(instanceId, WorkflowRunControl.NO_OP);
        try {
            JdbcResolvedConnection resolved = (JdbcResolvedConnection) connectionCatalog.resolve(DS_NAME, ConnectionKind.JDBC);
            String routingKey = resolved.connectionName();
            Assertions.assertTrue(routingKey.startsWith("snap:" + instanceId + ":"));

            String updatedUrl = "jdbc:h2:mem:hotreload-updated;DB_CLOSE_DELAY=-1";
            persistAndReload(updatedUrl);

            ExecutionConnectionSnapshot stillFrozen = taskExecutionService.getConnectionSnapshot(instanceId).orElseThrow();
            Assertions.assertEquals(originalUrl, stillFrozen.connections().getFirst().configParams().get("url"));

            try (var connection = resolved.dataSource().getConnection()) {
                Assertions.assertTrue(connection.getMetaData().getURL().contains("hotreload-original"));
            }
        } finally {
            WorkflowRunContext.clear();
        }
    }

    @Test
    @Transactional
    void newRunPicksUpPostReloadParams() throws Exception {
        persistAndReload("jdbc:h2:mem:hotreload-newrun-a;DB_CLOSE_DELAY=-1");
        persistAndReload("jdbc:h2:mem:hotreload-newrun-b;DB_CLOSE_DELAY=-1");

        JdbcResolvedConnection resolved = (JdbcResolvedConnection) connectionCatalog.resolve(DS_NAME, ConnectionKind.JDBC);
        try (var connection = resolved.dataSource().getConnection()) {
            Assertions.assertTrue(connection.getMetaData().getURL().contains("hotreload-newrun-b"));
        }
    }

    @Test
    @Transactional
    void reloadFailureMarksDegradedAndServesLastKnownGoodForNewRun() throws Exception {
        String goodUrl = "jdbc:h2:mem:hotreload-degraded-good;DB_CLOSE_DELAY=-1";
        persistAndReload(goodUrl);

        JdbcResolvedConnection good = (JdbcResolvedConnection) connectionCatalog.resolve(DS_NAME, ConnectionKind.JDBC);
        try (var connection = good.dataSource().getConnection()) {
            Assertions.assertTrue(connection.getMetaData().getURL().contains("hotreload-degraded-good"));
        }

        // Persist broken config but keep last-known-good pool registered (D-11).
        DataSourceConfigPO broken = dataSourceConfigRepository.findById(DS_NAME).orElseThrow();
        broken.setDriverClassName("com.example.NonexistentDriver");
        broken.setUpdatedAt(Instant.now());
        dataSourceConfigRepository.saveAndFlush(broken);

        CatalogEntry base = connectionCatalogImpl.findEntry(DS_NAME, ConnectionKind.JDBC).orElseThrow();
        CatalogEntry degraded = hotReloadCoordinator.reload(DS_NAME, ConnectionKind.JDBC, base);

        Assertions.assertEquals(ConnectionHealthStatus.DEGRADED, degraded.healthStatus());
        Assertions.assertNotNull(degraded.degradedReason());

        CatalogEntry listed = connectionCatalogImpl.listAll().stream()
                .filter(entry -> DS_NAME.equals(entry.name()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(ConnectionHealthStatus.DEGRADED, listed.healthStatus());

        JdbcResolvedConnection lastGood = (JdbcResolvedConnection) connectionCatalog.resolve(DS_NAME, ConnectionKind.JDBC);
        try (var connection = lastGood.dataSource().getConnection()) {
            Assertions.assertTrue(connection.getMetaData().getURL().contains("hotreload-degraded-good"));
        }
    }

    private void persistAndReload(String url) {
        dataSourceConfigService.save(DS_NAME, url, "sa", "", null, "org.h2.Driver", null);
        CatalogEntry entry = connectionCatalogImpl.reload(DS_NAME, ConnectionKind.JDBC);
        Assertions.assertEquals(ConnectionHealthStatus.HEALTHY, entry.healthStatus());
    }

    private static TemplateV2VO jdbcTemplate(String dataSourceId) {
        TemplateV2VO template = new TemplateV2VO();
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId(dataSourceId);
        template.getSources().put("src", source);
        return template;
    }
}
