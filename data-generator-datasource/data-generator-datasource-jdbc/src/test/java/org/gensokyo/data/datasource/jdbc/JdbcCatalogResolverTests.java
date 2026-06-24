/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.gensokyo.data.datasource.api.CatalogResolveSupport;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.JdbcResolvedConnection;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.secret.SecretResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for managed-first and inline JDBC endpoint resolution (D-18, D-20).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
class JdbcCatalogResolverTests {

    private ConnectionCatalog connectionCatalog;
    private SecretResolver secretResolver;
    private DynamicRoutingDataSource routing;
    private Map<String, DataSource> registeredPools;
    private JdbcCatalogResolver resolver;

    @BeforeEach
    void setUp() {
        connectionCatalog = mock(ConnectionCatalog.class);
        secretResolver = mock(SecretResolver.class);
        routing = mock(DynamicRoutingDataSource.class);
        registeredPools = new LinkedHashMap<>();
        when(routing.getDataSources()).thenReturn(registeredPools);
        doAnswer(invocation -> {
            registeredPools.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(routing).addDataSource(any(String.class), any(DataSource.class));
        @SuppressWarnings("unchecked")
        ObjectProvider<DynamicRoutingDataSource> routingProvider = mock(ObjectProvider.class);
        when(routingProvider.getIfAvailable()).thenReturn(routing);
        resolver = new JdbcCatalogResolver(connectionCatalog, routingProvider, secretResolver);
    }

    @Test
    void blankDataSourceIdWithInlineRegistersPoolKeyEqualToInlineName() {
        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName("inline-h2");
        inline.setUrl("jdbc:h2:mem:inline-h2;DB_CLOSE_DELAY=-1");
        inline.setDriverClassName("org.h2.Driver");
        inline.setUsername("sa");
        inline.setPassword("");

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSource(inline);

        when(secretResolver.resolveInlinePassword("", null)).thenReturn("");

        String resolved = resolver.resolveSourceDataSourceId(source);

        assertEquals("inline-h2", resolved);
        assertTrue(registeredPools.containsKey("inline-h2"));
        assertInstanceOf(DruidDataSource.class, registeredPools.get("inline-h2"));
    }

    @Test
    void unknownManagedIdThrowsIllegalArgumentExceptionContainingConnectionName() {
        String connectionName = "missing-primary";
        when(connectionCatalog.resolve(eq(connectionName), eq(ConnectionKind.JDBC)))
                .thenThrow(CatalogResolveSupport.unknownConnection(
                        connectionName, ConnectionKind.JDBC, "Register the connection in the console catalog."));

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId(connectionName);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveSourceDataSourceId(source));

        assertTrue(error.getMessage().contains(connectionName));
        assertTrue(error.getMessage().contains("JDBC"));
    }

    @Test
    void inlinePasswordSecretRefResolvesThroughSecretResolver() {
        InlineDataSourceVO inline = new InlineDataSourceVO();
        inline.setName("secret-inline");
        inline.setUrl("jdbc:h2:mem:secret-inline;DB_CLOSE_DELAY=-1");
        inline.setDriverClassName("org.h2.Driver");
        inline.setUsername("sa");
        inline.setPasswordSecretRef("db/password");

        QuerySourceVO source = new QuerySourceVO();
        source.setDataSource(inline);

        when(secretResolver.resolveInlinePassword(null, "db/password")).thenReturn("vault-password");

        String resolved = resolver.resolveSourceDataSourceId(source);

        assertEquals("secret-inline", resolved);
        verify(secretResolver).resolveInlinePassword(null, "db/password");
        DruidDataSource pool = (DruidDataSource) registeredPools.get("secret-inline");
        assertEquals("vault-password", pool.getPassword());
    }

    @Test
    void managedResolveRegistersCatalogPoolWhenAbsent() {
        DruidDataSource managedPool = new DruidDataSource();
        managedPool.setUrl("jdbc:h2:mem:managed;DB_CLOSE_DELAY=-1");
        managedPool.setDriverClassName("org.h2.Driver");

        when(connectionCatalog.resolve("primary", ConnectionKind.JDBC))
                .thenReturn(new JdbcResolvedConnection("primary", managedPool));

        String resolved = resolver.resolveManagedDataSourceId("primary");

        assertEquals("primary", resolved);
        assertTrue(registeredPools.containsKey("primary"));
    }
}
