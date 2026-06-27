/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.JdbcCatalogMetadata;
import org.gensokyo.data.datasource.BundledJdbcDriverRegistry;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.messaging.MessagingClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ConsoleDataSourceController}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@ExtendWith(MockitoExtension.class)
class ConsoleDataSourceControllerTest {

    @Mock
    private DataSourceConfigService dataSourceConfigService;

    @Mock
    private BundledJdbcDriverRegistry bundledJdbcDriverRegistry;

    @Mock
    private MessagingClusterConfigService messagingClusterConfigService;

    @Mock
    private ConnectionCatalog connectionCatalog;

    @Mock
    private DataGeneratorProperties properties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(properties.getGovernance()).thenReturn(new DataGeneratorProperties.Governance());
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConsoleDataSourceController(
                                dataSourceConfigService,
                                bundledJdbcDriverRegistry,
                                messagingClusterConfigService,
                                connectionCatalog,
                                properties))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void overview_includesDriverPresets() throws Exception {
        when(dataSourceConfigService.listAll()).thenReturn(List.of());
        when(dataSourceConfigService.listRuntimeNames()).thenReturn(Set.of("h2"));
        when(bundledJdbcDriverRegistry.hasBundle(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            return "mysql".equals(key) || "dm".equals(key);
        });
        when(messagingClusterConfigService.listKafkaClusterKeys()).thenReturn(List.of());
        when(messagingClusterConfigService.listElasticsearchClusterKeys()).thenReturn(List.of());
        when(messagingClusterConfigService.listKafka()).thenReturn(List.of());
        when(messagingClusterConfigService.listElasticsearch()).thenReturn(List.of());
        when(connectionCatalog.listAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.driverPresets[0].id").exists())
                .andExpect(jsonPath("$.data.driverPresets[?(@.id == 'mysql8')].bundled").value(true));
    }

    @Test
    void overview_includesCatalogConnectionsWithSource() throws Exception {
        when(dataSourceConfigService.listAll()).thenReturn(List.of());
        when(dataSourceConfigService.listRuntimeNames()).thenReturn(Set.of("data-generator"));
        when(bundledJdbcDriverRegistry.hasBundle(anyString())).thenReturn(false);
        when(messagingClusterConfigService.listKafkaClusterKeys()).thenReturn(List.of());
        when(messagingClusterConfigService.listElasticsearchClusterKeys()).thenReturn(List.of());
        when(messagingClusterConfigService.listKafka()).thenReturn(List.of());
        when(messagingClusterConfigService.listElasticsearch()).thenReturn(List.of());
        when(connectionCatalog.listAll()).thenReturn(List.of(
                new CatalogEntry(
                        "data-generator",
                        ConnectionKind.JDBC,
                        CatalogEntrySource.BOOTSTRAP,
                        new JdbcCatalogMetadata("jdbc:h2:mem:dg", "org.h2.Driver"))));

        mockMvc.perform(get("/api/datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.catalogConnections[0].name").value("data-generator"))
                .andExpect(jsonPath("$.data.catalogConnections[0].kind").value("JDBC"))
                .andExpect(jsonPath("$.data.catalogConnections[0].source").value("BOOTSTRAP"));
    }

    @Test
    void driverPresets_returnsCatalog() throws Exception {
        when(bundledJdbcDriverRegistry.hasBundle(anyString())).thenReturn(true);
        mockMvc.perform(get("/api/datasources/driver-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.id == 'dm8')].driverClassName").value("dm.jdbc.driver.DmDriver"))
                .andExpect(jsonPath("$.data[?(@.id == 'dm8')].bundled").value(true));
    }
}
