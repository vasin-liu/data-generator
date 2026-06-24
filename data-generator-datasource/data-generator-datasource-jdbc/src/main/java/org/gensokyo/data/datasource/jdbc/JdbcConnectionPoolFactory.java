/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.secret.SecretResolver;
import org.gensokyo.kit.character.StrKit;

import java.util.Objects;

/**
 * Builds {@link DruidDataSource} pools from inline template JDBC blocks (D-19, D-22).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public final class JdbcConnectionPoolFactory {

    private JdbcConnectionPoolFactory() {
    }

    /**
     * Creates a Druid pool for an inline datasource definition.
     *
     * @param inline         inline JDBC endpoint from a template
     * @param secretResolver resolver for {@code passwordSecretRef}
     * @return configured Druid pool (not yet registered with routing)
     * @throws IllegalArgumentException when required inline fields are blank
     */
    public static DruidDataSource createInlinePool(InlineDataSourceVO inline, SecretResolver secretResolver) {
        Objects.requireNonNull(inline, "inline");
        Objects.requireNonNull(secretResolver, "secretResolver");
        if (StrKit.isBlank(inline.getUrl())) {
            throw new IllegalArgumentException("Inline datasource url must not be blank");
        }
        if (StrKit.isBlank(inline.getDriverClassName())) {
            throw new IllegalArgumentException("Inline datasource driverClassName must not be blank");
        }
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(inline.getUrl());
        dataSource.setUsername(inline.getUsername());
        // Secret ref wins over plaintext when present (D-22).
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
