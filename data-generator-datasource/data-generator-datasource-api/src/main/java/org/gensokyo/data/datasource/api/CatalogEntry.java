/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable catalog list entry in the shared global namespace (D-01, D-02, D-06, D-08, D-26).
 * Metadata payloads are display-only and must not contain secret values (D-10).
 *
 * @param name            connection name unique across kinds in the global namespace
 * @param kind            connection type discriminator
 * @param source          bootstrap vs managed origin
 * @param metadata        kind-specific non-secret metadata for list views
 * @param version         monotonic catalog generation used for run-start snapshot pinning (D-08)
 * @param updatedAt       last config mutation timestamp when known
 * @param healthStatus    HEALTHY or DEGRADED after reload/connectivity events (D-26)
 * @param lastReloadAt    timestamp of the most recent hot-reload attempt
 * @param degradedReason  operator-facing failure summary when {@code healthStatus} is DEGRADED
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public record CatalogEntry(
        String name,
        ConnectionKind kind,
        CatalogEntrySource source,
        CatalogMetadata metadata,
        long version,
        Instant updatedAt,
        ConnectionHealthStatus healthStatus,
        Instant lastReloadAt,
        String degradedReason) {

    /**
     * Creates a catalog entry with default version and HEALTHY status (Phase 6 compatibility).
     *
     * @param name     connection name
     * @param kind     connection kind
     * @param source   bootstrap or managed origin
     * @param metadata display metadata without secrets
     */
    public CatalogEntry(String name, ConnectionKind kind, CatalogEntrySource source, CatalogMetadata metadata) {
        this(name, kind, source, metadata, 1L, null, ConnectionHealthStatus.HEALTHY, null, null);
    }

    /**
     * Compact constructor validating required fields.
     */
    public CatalogEntry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(healthStatus, "healthStatus");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
