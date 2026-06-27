/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.elasticsearch.DynamicElasticsearchClientRegistry;
import org.gensokyo.data.datasource.kafka.DynamicKafkaTemplateRegistry;
import org.gensokyo.data.repository.MessagingClusterConfigRepository;
import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.gensokyo.data.repository.DataSourceConfigRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConnectionCatalogImpl} merge semantics.
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
@ExtendWith(MockitoExtension.class)
class ConnectionCatalogImplTests {

    @Mock
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Mock
    private MessagingClusterConfigRepository messagingClusterConfigRepository;

    @Mock
    private HotReloadCoordinator hotReloadCoordinator;

    private ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider;
    private ObjectProvider<DynamicKafkaTemplateRegistry> kafkaRegistryProvider;
    private ObjectProvider<DynamicElasticsearchClientRegistry> elasticsearchRegistryProvider;
    private ConnectionCatalogImpl connectionCatalog;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Named mocks avoid Mockito cross-stubbing on erased ObjectProvider#getIfAvailable().
        dynamicRoutingDataSourceProvider = mock(
                ObjectProvider.class, withSettings().name("jdbcObjectProvider"));
        kafkaRegistryProvider = mock(
                ObjectProvider.class, withSettings().name("kafkaObjectProvider"));
        elasticsearchRegistryProvider = mock(
                ObjectProvider.class, withSettings().name("esObjectProvider"));
        connectionCatalog = new ConnectionCatalogImpl(
                dynamicRoutingDataSourceProvider,
                kafkaRegistryProvider,
                elasticsearchRegistryProvider,
                dataSourceConfigRepository,
                messagingClusterConfigRepository,
                hotReloadCoordinator);
        lenient().when(kafkaRegistryProvider.getIfAvailable()).thenReturn(null);
        lenient().when(elasticsearchRegistryProvider.getIfAvailable()).thenReturn(null);
        lenient().when(messagingClusterConfigRepository.findByClusterType(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
    }

    @Test
    void listAll_managedJdbcEntryOverridesBootstrapForSameName() {
        DataSourceConfigPO managed = new DataSourceConfigPO();
        managed.setName("shared-ds");
        managed.setUrl("jdbc:h2:mem:managed");
        managed.setDriverClassName("org.h2.Driver");
        managed.setEnabled(Boolean.TRUE);
        managed.setUpdatedAt(Instant.now());

        DynamicRoutingDataSource routing = mock(DynamicRoutingDataSource.class);
        when(routing.getDataSources()).thenReturn(Map.of("shared-ds", mock(DataSource.class)));
        when(dynamicRoutingDataSourceProvider.getIfAvailable()).thenReturn(routing);
        when(dataSourceConfigRepository.findByEnabledTrue()).thenReturn(List.of(managed));
        when(dataSourceConfigRepository.findById("shared-ds")).thenReturn(Optional.of(managed));

        CatalogEntry entry = connectionCatalog.listAll().stream()
                .filter(candidate -> "shared-ds".equals(candidate.name()))
                .findFirst()
                .orElseThrow();

        Assertions.assertEquals(ConnectionKind.JDBC, entry.kind());
        Assertions.assertEquals(CatalogEntrySource.MANAGED, entry.source());
    }

    @Test
    void listAll_yamlOnlyJdbcTaggedBootstrap() {
        DynamicRoutingDataSource routing = mock(DynamicRoutingDataSource.class);
        when(routing.getDataSources()).thenReturn(Map.of("yaml-only", mock(DataSource.class)));
        when(dynamicRoutingDataSourceProvider.getIfAvailable()).thenReturn(routing);
        when(dataSourceConfigRepository.findByEnabledTrue()).thenReturn(List.of());
        when(dataSourceConfigRepository.findById("yaml-only")).thenReturn(Optional.empty());

        CatalogEntry entry = connectionCatalog.listAll().stream()
                .filter(candidate -> "yaml-only".equals(candidate.name()))
                .findFirst()
                .orElseThrow();

        Assertions.assertEquals(CatalogEntrySource.BOOTSTRAP, entry.source());
    }
}
