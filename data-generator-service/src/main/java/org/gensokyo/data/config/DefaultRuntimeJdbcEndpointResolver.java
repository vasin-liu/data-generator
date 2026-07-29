/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.datasource.api.CatalogResolveSupport;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.datasource.api.JdbcResolvedConnection;
import org.gensokyo.data.datasource.api.ResolvedConnection;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.secret.SecretResolver;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Registers inline JDBC endpoints into the dynamic routing datasource and validates managed ids via catalog (D-29).
 *
 * <p>This bean is the <b>V2 Template execute-path authority</b> for JDBC endpoint resolution:
 * {@link org.gensokyo.data.calcite.source.QuerySourceFactory}, PostGIS source factories, and
 * {@code JdbcRowSinkAdapter} call it to resolve the routing key used for both catalog-managed
 * and inline connections at run time. When a {@code WorkflowRunContext} is bound for the
 * current run (i.e. {@code instanceId} is set), the managed path returns the run-start
 * snapshot routing key {@code snap:{instanceId}:{name}} instead of the logical
 * {@code dataSourceId}, so in-flight runs keep their pre-reload pool (DS-03).
 *
 * <p>{@link org.gensokyo.data.datasource.jdbc.JdbcCatalogResolver} is a separate resolver that
 * remains the datasource module's catalog-side resolution helper. Both resolvers coexist by
 * design in this phase — this class does not delegate to or depend on
 * {@code JdbcCatalogResolver}; it independently mirrors the same catalog-resolve /
 * register-if-absent semantics for the execute path. Consolidating the two is a deferred,
 * future concern.
 *
 * <p>Maintainer ownership model and call-site inventory:
 * {@code docs/jdbc-resolver-ownership.md}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@RequiredArgsConstructor
public class DefaultRuntimeJdbcEndpointResolver implements RuntimeJdbcEndpointResolver {

    private final ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider;
    private final SecretResolver secretResolver;
    private final ObjectProvider<ConnectionCatalog> connectionCatalogProvider;

    @Override
    public String resolveSourceDataSourceId(QuerySourceVO source) {
        if (source == null) {
            return null;
        }
        if (StrKit.isNotBlank(source.getDataSourceId())) {
            return resolveManagedDataSourceId(source.getDataSourceId());
        }
        return ensureInlineDataSource(source.getDataSource(), source.getDataSourceId());
    }

    @Override
    public String resolveSinkDataSourceId(JdbcWriterVO writer) {
        if (writer == null) {
            return null;
        }
        if (StrKit.isNotBlank(writer.getDataSourceId())) {
            return resolveManagedDataSourceId(writer.getDataSourceId());
        }
        return ensureInlineDataSource(writer.getDataSource(), writer.getDataSourceId());
    }

    /**
     * Resolves a managed catalog JDBC connection for the V2 execute path and registers the
     * resolved pool under its routing key before returning it.
     *
     * <p>When a {@code WorkflowRunContext} is bound for the current thread, {@link
     * ConnectionCatalog#resolve} returns a snapshot-scoped connection whose {@code
     * connectionName()} is the {@code snap:{instanceId}:{name}} routing key, not the logical
     * {@code dataSourceId}. This method always returns that resolved name — never the logical
     * id — so in-flight runs keep routing to their run-start pool even if the catalog is
     * hot-reloaded mid-flight (DS-03). Resolve failures fail fast; there is no fallback to the
     * logical catalog name.
     *
     * @param dataSourceId managed connection name configured on the template
     * @return the resolved routing key used to select/register the JDBC pool, or {@code
     *     dataSourceId} unchanged when it is blank
     * @throws IllegalStateException if the connection catalog is unavailable
     * @throws IllegalArgumentException if the catalog entry is unknown or not a JDBC connection
     */
    private String resolveManagedDataSourceId(String dataSourceId) {
        if (StrKit.isBlank(dataSourceId)) {
            return dataSourceId;
        }
        ConnectionCatalog catalog = connectionCatalogProvider == null ? null : connectionCatalogProvider.getIfAvailable();
        if (catalog == null) {
            throw new IllegalStateException(
                    "ConnectionCatalog is required to resolve managed JDBC connection '" + dataSourceId + "'");
        }
        ResolvedConnection resolved = catalog.resolve(dataSourceId, ConnectionKind.JDBC);
        if (!(resolved instanceof JdbcResolvedConnection jdbc)) {
            throw CatalogResolveSupport.unknownConnection(
                    dataSourceId, ConnectionKind.JDBC, "Catalog entry is not a JDBC connection");
        }
        registerIfAbsent(jdbc.connectionName(), jdbc.dataSource());
        return jdbc.connectionName();
    }

    private void registerIfAbsent(String connectionName, DataSource dataSource) {
        DynamicRoutingDataSource routing = requireRouting();
        if (!routing.getDataSources().containsKey(connectionName)) {
            routing.addDataSource(connectionName, dataSource);
        }
    }

    private DynamicRoutingDataSource requireRouting() {
        DynamicRoutingDataSource routing = dynamicRoutingDataSourceProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("DynamicRoutingDataSource is required for JDBC endpoint loading");
        }
        return routing;
    }

    private String ensureInlineDataSource(InlineDataSourceVO inline, String fallback) {
        if (inline == null || StrKit.isBlank(inline.getName())) {
            return fallback;
        }
        DynamicRoutingDataSource routing = dynamicRoutingDataSourceProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("DynamicRoutingDataSource is required for inline JDBC endpoint loading");
        }
        if (!routing.getDataSources().containsKey(inline.getName())) {
            routing.addDataSource(inline.getName(), createDataSource(inline));
        }
        return inline.getName();
    }

    private DruidDataSource createDataSource(InlineDataSourceVO inline) {
        if (StrKit.isBlank(inline.getUrl())) {
            throw new IllegalArgumentException("Inline datasource url must not be blank");
        }
        if (StrKit.isBlank(inline.getDriverClassName())) {
            throw new IllegalArgumentException("Inline datasource driverClassName must not be blank");
        }
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(inline.getUrl());
        dataSource.setUsername(inline.getUsername());
        dataSource.setPassword(secretResolver.resolveInlinePassword(inline.getPassword(), inline.getPasswordSecretRef()));
        dataSource.setDriverClassName(inline.getDriverClassName());
        dataSource.setValidationQuery("SELECT 1");
        if (Objects.nonNull(inline.getProperties())) {
            inline.getProperties().forEach((key, value) -> {
                if (StrKit.isNotBlank(key) && value != null) {
                    dataSource.addConnectionProperty(key, value);
                }
            });
        }
        return dataSource;
    }
}
