/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.RequiredArgsConstructor;
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

/**
 * JDBC endpoint resolver with managed catalog lookup first and inline pool fallback (D-19, D-20).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
@RequiredArgsConstructor
public class JdbcCatalogResolver {

    private final ConnectionCatalog connectionCatalog;
    private final ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider;
    private final SecretResolver secretResolver;

    /**
     * Resolves the routing datasource key for a JDBC query source.
     *
     * @param source template query source (may be null)
     * @return routing key, or null when source is null
     */
    public String resolveSourceDataSourceId(QuerySourceVO source) {
        if (source == null) {
            return null;
        }
        if (StrKit.isNotBlank(source.getDataSourceId())) {
            return resolveManagedDataSourceId(source.getDataSourceId());
        }
        return ensureInlineDataSource(source.getDataSource(), source.getDataSourceId());
    }

    /**
     * Resolves the routing datasource key for a JDBC sink writer.
     *
     * @param writer JDBC writer configuration (may be null)
     * @return routing key, or null when writer is null
     */
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
     * Resolves a managed catalog JDBC connection and ensures it is registered for routing.
     *
     * @param dataSourceId managed connection name
     * @return catalog connection name used as routing key
     * @throws IllegalArgumentException when the catalog entry is unknown or not JDBC (D-07)
     */
    public String resolveManagedDataSourceId(String dataSourceId) {
        if (StrKit.isBlank(dataSourceId)) {
            return dataSourceId;
        }
        ResolvedConnection resolved = connectionCatalog.resolve(dataSourceId, ConnectionKind.JDBC);
        if (!(resolved instanceof JdbcResolvedConnection jdbc)) {
            throw CatalogResolveSupport.unknownConnection(
                    dataSourceId, ConnectionKind.JDBC, "Catalog entry is not a JDBC connection");
        }
        registerIfAbsent(jdbc.connectionName(), jdbc.dataSource());
        return jdbc.connectionName();
    }

    private String ensureInlineDataSource(InlineDataSourceVO inline, String fallback) {
        if (inline == null || StrKit.isBlank(inline.getName())) {
            return fallback;
        }
        DynamicRoutingDataSource routing = requireRouting();
        if (!routing.getDataSources().containsKey(inline.getName())) {
            DruidDataSource pool = JdbcConnectionPoolFactory.createInlinePool(inline, secretResolver);
            routing.addDataSource(inline.getName(), pool);
        }
        return inline.getName();
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
}
