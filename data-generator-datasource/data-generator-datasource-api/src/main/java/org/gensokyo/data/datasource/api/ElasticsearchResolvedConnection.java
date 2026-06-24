/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import java.util.Objects;

/**
 * Elasticsearch runtime resolution result. The client is an opaque adapter-owned object
 * (typically {@code RestClient}) to keep this API module free of Elasticsearch client dependencies.
 *
 * @param connectionName catalog entry name
 * @param clientHandle   opaque REST client from the elasticsearch adapter
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public record ElasticsearchResolvedConnection(String connectionName, Object clientHandle) implements ResolvedConnection {

    /**
     * Compact constructor validating required fields.
     */
    public ElasticsearchResolvedConnection {
        Objects.requireNonNull(connectionName, "connectionName");
        Objects.requireNonNull(clientHandle, "clientHandle");
        if (connectionName.isBlank()) {
            throw new IllegalArgumentException("connectionName must not be blank");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionKind kind() {
        return ConnectionKind.ELASTICSEARCH;
    }
}
