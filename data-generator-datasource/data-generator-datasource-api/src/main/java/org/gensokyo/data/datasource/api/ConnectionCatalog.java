/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import java.util.List;
import java.util.Optional;

/**
 * Connection catalog for JDBC, Kafka, and Elasticsearch (D-05, D-07, D-18, D-26).
 * CRUD mutations remain on existing service paths; implementations merge bootstrap and managed entries (D-06).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public interface ConnectionCatalog {

    /**
     * Resolves a live runtime handle for the given name and kind.
     *
     * @param name connection name in the shared global namespace
     * @param kind expected connection kind (must match the stored entry)
     * @return kind-specific {@link ResolvedConnection}
     * @throws IllegalArgumentException when the name is unknown, blank, or the stored kind does not match;
     *                                  message includes name, kind, and operator hints (D-07)
     */
    ResolvedConnection resolve(String name, ConnectionKind kind);

    /**
     * Returns the merged bootstrap + managed catalog view for operator listing (D-06, D-11).
     * Managed entries override bootstrap entries with the same name (D-23).
     * List payloads must not include secret values (D-10).
     *
     * @return immutable list of catalog entries
     */
    List<CatalogEntry> listAll();

    /**
     * Tests connectivity for a named catalog entry or an unsaved draft configuration (D-18, D-20).
     *
     * @param request existing-entry or draft payload test input
     * @return actionable success/failure result without secret values in {@code details}
     */
    ConnectionTestResult test(ConnectionTestRequest request);

    /**
     * Hot-reloads runtime registries after a datasource save (D-09, D-11).
     * On failure the persisted DB config is retained, the entry becomes {@link ConnectionHealthStatus#DEGRADED},
     * and runtime continues serving the last known good handles for new runs until connectivity is restored.
     *
     * @param name connection name
     * @param kind expected connection kind
     * @return post-reload catalog entry including version and health metadata
     */
    CatalogEntry reload(String name, ConnectionKind kind);

    /**
     * Looks up a catalog entry for version pinning at run start (D-08).
     *
     * @param name connection name
     * @param kind expected connection kind
     * @return matching entry when present in the merged catalog view
     */
    default Optional<CatalogEntry> findEntry(String name, ConnectionKind kind) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        return listAll().stream()
                .filter(entry -> entry.kind() == kind && entry.name().equals(trimmed))
                .findFirst();
    }
}
