/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api.snapshot;

import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionKind;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * One connection reference frozen at run start with param-only config and catalog version pinning (D-01, D-05, D-08).
 * {@code configParams} holds JDBC url/username/secretRef/driver, Kafka bootstrap/cluster, or ES hosts/secretRef —
 * never resolved pool handles or plaintext secrets.
 *
 * @param name              connection name in the global namespace
 * @param kind              JDBC, KAFKA, or ELASTICSEARCH
 * @param source            BOOTSTRAP vs MANAGED origin tag (D-06)
 * @param catalogVersion    {@link org.gensokyo.data.datasource.api.CatalogEntry#version()} pinned at RUNNING
 * @param catalogUpdatedAt  {@link org.gensokyo.data.datasource.api.CatalogEntry#updatedAt()} pinned at RUNNING
 * @param configParams      kind-specific parameter map without resolved runtime handles
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public record SnapshottedConnectionRef(
        String name,
        ConnectionKind kind,
        CatalogEntrySource source,
        long catalogVersion,
        Instant catalogUpdatedAt,
        Map<String, Object> configParams) {

    /**
     * Compact constructor validating required fields and copying config params.
     */
    public SnapshottedConnectionRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(configParams, "configParams");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        // Defensive copy so snapshot immutability survives serde round-trips.
        configParams = Map.copyOf(configParams);
    }
}
