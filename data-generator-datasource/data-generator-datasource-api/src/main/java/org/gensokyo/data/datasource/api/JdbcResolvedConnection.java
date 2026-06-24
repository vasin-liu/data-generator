/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * JDBC runtime resolution result.
 *
 * @param connectionName catalog entry name
 * @param dataSource     pooled or routing datasource handle
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public record JdbcResolvedConnection(String connectionName, DataSource dataSource) implements ResolvedConnection {

    /**
     * Compact constructor validating required fields.
     */
    public JdbcResolvedConnection {
        Objects.requireNonNull(connectionName, "connectionName");
        Objects.requireNonNull(dataSource, "dataSource");
        if (connectionName.isBlank()) {
            throw new IllegalArgumentException("connectionName must not be blank");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionKind kind() {
        return ConnectionKind.JDBC;
    }
}
