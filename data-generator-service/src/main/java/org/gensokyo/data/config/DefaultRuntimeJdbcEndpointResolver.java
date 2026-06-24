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
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.secret.SecretResolver;
import org.gensokyo.kit.character.StrKit;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Objects;

/**
 * Registers inline JDBC endpoints into the dynamic routing datasource and validates managed ids via catalog (D-29).
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
            ensureManagedJdbc(source.getDataSourceId());
            return source.getDataSourceId();
        }
        return ensureInlineDataSource(source.getDataSource(), source.getDataSourceId());
    }

    @Override
    public String resolveSinkDataSourceId(JdbcWriterVO writer) {
        if (writer == null) {
            return null;
        }
        if (StrKit.isNotBlank(writer.getDataSourceId())) {
            ensureManagedJdbc(writer.getDataSourceId());
            return writer.getDataSourceId();
        }
        return ensureInlineDataSource(writer.getDataSource(), writer.getDataSourceId());
    }

    private void ensureManagedJdbc(String name) {
        ConnectionCatalog catalog = connectionCatalogProvider == null ? null : connectionCatalogProvider.getIfAvailable();
        if (catalog != null) {
            catalog.resolve(name, ConnectionKind.JDBC);
        }
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
