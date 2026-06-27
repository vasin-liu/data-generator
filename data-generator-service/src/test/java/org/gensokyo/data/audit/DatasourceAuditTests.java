/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.audit;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.ConnectionHealthStatus;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.catalog.ConnectionCatalogImpl;
import org.gensokyo.data.datasource.catalog.HotReloadCoordinator;
import org.gensokyo.data.model.po.AuditEventPO;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.repository.AuditEventRepository;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Datasource audit event types and sanitizer guarantees (D-22..D-24).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class DatasourceAuditTests {

    private static final String DS_NAME = "audit-it-ds";

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ConnectionCatalogImpl connectionCatalogImpl;

    @Autowired
    private HotReloadCoordinator hotReloadCoordinator;

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @BeforeEach
    void cleanup() {
        dataSourceConfigRepository.findById(DS_NAME).ifPresent(dataSourceConfigRepository::delete);
        if (dynamicRoutingDataSource.getDataSources().containsKey(DS_NAME)) {
            dynamicRoutingDataSource.removeDataSource(DS_NAME);
        }
    }

    @Test
    @Transactional
    void hotReloadSuccess_emitsReloadAudit() {
        persistHealthyDatasource();
        CatalogEntry base = connectionCatalogImpl.findEntry(DS_NAME, ConnectionKind.JDBC).orElseThrow();
        hotReloadCoordinator.reload(DS_NAME, ConnectionKind.JDBC, base);

        List<AuditEventPO> reloadEvents = auditEventRepository.findByActionOrderByOccurredAtDesc(
                DatasourceAuditActions.RELOAD, org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        Assertions.assertFalse(reloadEvents.isEmpty());
        AuditEventPO latest = reloadEvents.getFirst();
        Assertions.assertEquals(DatasourceAuditActions.CATEGORY, latest.getResourceType());
        Assertions.assertFalse(latest.getDetailJson().contains("password"));
        Assertions.assertFalse(latest.getDetailJson().contains("jdbc:"));
    }

    @Test
    @Transactional
    void hotReloadFailure_emitsReloadAndDegradedAudit() {
        persistHealthyDatasource();
        DataSourceConfigPO broken = dataSourceConfigRepository.findById(DS_NAME).orElseThrow();
        broken.setDriverClassName("com.example.NonexistentDriver");
        broken.setUpdatedAt(Instant.now());
        dataSourceConfigRepository.saveAndFlush(broken);

        CatalogEntry base = connectionCatalogImpl.findEntry(DS_NAME, ConnectionKind.JDBC).orElseThrow();
        CatalogEntry result = hotReloadCoordinator.reload(DS_NAME, ConnectionKind.JDBC, base);

        Assertions.assertEquals(ConnectionHealthStatus.DEGRADED, result.healthStatus());

        List<AuditEventPO> reloadEvents = auditEventRepository.findByActionOrderByOccurredAtDesc(
                DatasourceAuditActions.RELOAD, org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
        Assertions.assertTrue(reloadEvents.stream().anyMatch(e -> DS_NAME.equals(e.getResourceId())));

        List<AuditEventPO> degradedEvents = auditEventRepository.findByActionOrderByOccurredAtDesc(
                DatasourceAuditActions.DEGRADED, org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
        Assertions.assertTrue(degradedEvents.stream().anyMatch(e -> DS_NAME.equals(e.getResourceId())));
    }

    @Test
    void auditDetailSanitizer_redactsSensitiveKeys() {
        Map<String, Object> sanitized = AuditDetailSanitizer.sanitizeJson("""
                {"connectionName":"demo","password":"secret","apiKey":"key","url":"jdbc:mysql://user:pass@host/db"}
                """);
        Assertions.assertEquals("[redacted]", sanitized.get("password"));
        Assertions.assertEquals("[redacted]", sanitized.get("apiKey"));
        Assertions.assertEquals("demo", sanitized.get("connectionName"));
    }

    private void persistHealthyDatasource() {
        DataSourceConfigPO row = new DataSourceConfigPO();
        row.setName(DS_NAME);
        row.setUrl("jdbc:h2:mem:audit-it;DB_CLOSE_DELAY=-1");
        row.setUsername("sa");
        row.setPassword("");
        row.setDriverClassName("org.h2.Driver");
        row.setEnabled(Boolean.TRUE);
        row.setCreatedAt(Instant.now());
        row.setUpdatedAt(Instant.now());
        dataSourceConfigRepository.saveAndFlush(row);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(row.getUrl());
        dataSource.setUsername(row.getUsername());
        dataSource.setPassword(row.getPassword());
        dataSource.setDriverClassName(row.getDriverClassName());
        dynamicRoutingDataSource.addDataSource(DS_NAME, dataSource);
    }
}
