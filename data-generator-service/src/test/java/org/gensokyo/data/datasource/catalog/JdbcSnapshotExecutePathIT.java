/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.runtime.WorkflowRunContext;
import org.gensokyo.data.calcite.runtime.WorkflowRunControl;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionHealthStatus;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.gensokyo.data.task.TaskExecutionService;
import org.gensokyo.data.util.RandomKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mid-flight JDBC execute-path snapshot routing proof for DS-03 (D-09..D-12).
 *
 * <p>Proves that {@link RuntimeJdbcEndpointResolver#resolveSourceDataSourceId} and
 * {@link RuntimeJdbcEndpointResolver#resolveSinkDataSourceId} — the V2 execute-path
 * authority used by {@code QuerySourceFactory}, {@code PostGisQuerySourceFactory}, and
 * {@code JdbcRowSinkAdapter} — return {@code snap:{instanceId}:{name}} routing keys for
 * both source and sink once a {@link WorkflowRunContext} is bound, and that those keys
 * are unaffected by a mid-flight catalog reload of the same managed connection
 * (in-flight isolation, DS-03).
 *
 * <p>Kept as a focused class separate from {@link HotReloadTests} (which covers
 * catalog-level DEGRADED / hot-reload scenarios) to avoid bloating that suite with
 * duplicate resolver-level assertions. Kafka and Elasticsearch snapshot routing paths
 * are unchanged by this phase and are not exercised here.
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-07-24
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class JdbcSnapshotExecutePathIT {

    private static final String DS_NAME = "jdbc-snap-execute-it-ds";

    @Autowired
    private ConnectionCatalogImpl connectionCatalogImpl;

    @Autowired
    private ConnectionCatalog connectionCatalog;

    @Autowired
    private RuntimeJdbcEndpointResolver runtimeJdbcEndpointResolver;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @BeforeEach
    void resetDatasourceRow() {
        dataSourceConfigRepository.findById(DS_NAME).ifPresent(dataSourceConfigRepository::delete);
        if (dynamicRoutingDataSource.getDataSources().containsKey(DS_NAME)) {
            dynamicRoutingDataSource.removeDataSource(DS_NAME);
        }
    }

    /**
     * Verifies that both source and sink managed JDBC resolution return {@code snap:}
     * routing keys while a run is in flight, and that those keys are unchanged after a
     * mid-flight catalog reload of the same connection (D-09, D-10, D-11, D-12).
     */
    @Test
    @Transactional
    void sourceAndSinkResolveRetainSnapshotKeyAcrossMidFlightReload() {
        String originalUrl = "jdbc:h2:mem:jdbc-snap-execute-original;DB_CLOSE_DELAY=-1";
        persistAndReload(originalUrl);

        Long instanceId = RandomKit.snowFlake().nextId();
        taskExecutionService.queueExecution(20L, "jdbc-snap-execute-it", instanceId, "V2");

        TemplateV2VO template = jdbcSourceAndSinkTemplate(DS_NAME);
        QuerySourceVO source = (QuerySourceVO) template.getSources().get("src");
        JdbcWriterVO writer = (JdbcWriterVO) template.getSinks().getFirst().getWriters().getFirst();

        taskExecutionService.markRunning(instanceId);
        taskExecutionService.captureConnectionSnapshot(instanceId, template, connectionCatalog);

        WorkflowRunContext.bind(instanceId, WorkflowRunControl.NO_OP);
        try {
            String expectedPrefix = "snap:" + instanceId + ":";

            String sourceKey = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(source);
            String sinkKey = runtimeJdbcEndpointResolver.resolveSinkDataSourceId(writer);
            Assertions.assertTrue(sourceKey.startsWith(expectedPrefix),
                    "source resolve did not return a snapshot routing key");
            Assertions.assertTrue(sinkKey.startsWith(expectedPrefix),
                    "sink resolve did not return a snapshot routing key");

            // Mid-flight reload must not redirect the in-flight run's routing keys (D-11).
            String updatedUrl = "jdbc:h2:mem:jdbc-snap-execute-updated;DB_CLOSE_DELAY=-1";
            persistAndReload(updatedUrl);

            String sourceKeyAfterReload = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(source);
            String sinkKeyAfterReload = runtimeJdbcEndpointResolver.resolveSinkDataSourceId(writer);
            Assertions.assertTrue(sourceKeyAfterReload.startsWith(expectedPrefix),
                    "source resolve lost its snapshot routing key after mid-flight reload");
            Assertions.assertTrue(sinkKeyAfterReload.startsWith(expectedPrefix),
                    "sink resolve lost its snapshot routing key after mid-flight reload");
            Assertions.assertEquals(sourceKey, sourceKeyAfterReload,
                    "source routing key must not change across mid-flight reload");
            Assertions.assertEquals(sinkKey, sinkKeyAfterReload,
                    "sink routing key must not change across mid-flight reload");
        } finally {
            WorkflowRunContext.clear();
        }

        // After clear, unbound resolve returns the live catalog key (not snap:).
        String liveSourceKey = runtimeJdbcEndpointResolver.resolveSourceDataSourceId(source);
        String liveSinkKey = runtimeJdbcEndpointResolver.resolveSinkDataSourceId(writer);
        Assertions.assertFalse(liveSourceKey.startsWith("snap:"),
                "source resolve without bound context must not return a snapshot key");
        Assertions.assertFalse(liveSinkKey.startsWith("snap:"),
                "sink resolve without bound context must not return a snapshot key");
        Assertions.assertEquals(DS_NAME, liveSourceKey);
        Assertions.assertEquals(DS_NAME, liveSinkKey);
    }

    private void persistAndReload(String url) {
        dataSourceConfigService.save(DS_NAME, url, "sa", "", null, "org.h2.Driver", null, null);
        CatalogEntry entry = connectionCatalogImpl.reload(DS_NAME, ConnectionKind.JDBC);
        Assertions.assertEquals(ConnectionHealthStatus.HEALTHY, entry.healthStatus());
    }

    private static TemplateV2VO jdbcSourceAndSinkTemplate(String dataSourceId) {
        TemplateV2VO template = new TemplateV2VO();
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId(dataSourceId);
        template.getSources().put("src", source);

        JdbcWriterVO writer = new JdbcWriterVO();
        writer.setDataSourceId(dataSourceId);
        WriteStageVO sinkStage = new WriteStageVO();
        sinkStage.setWriters(List.of(writer));
        template.setSinks(List.of(sinkStage));

        return template;
    }
}
