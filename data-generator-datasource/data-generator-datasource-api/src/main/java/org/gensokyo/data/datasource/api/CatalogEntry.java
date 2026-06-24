/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import java.util.Objects;

/**
 * Immutable catalog list entry in the shared global namespace (D-01, D-02, D-06).
 * Metadata payloads are display-only and must not contain secret values (D-10).
 *
 * @param name     connection name unique across kinds in the global namespace
 * @param kind     connection type discriminator
 * @param source   bootstrap vs managed origin
 * @param metadata kind-specific non-secret metadata for list views
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public record CatalogEntry(
        String name,
        ConnectionKind kind,
        CatalogEntrySource source,
        CatalogMetadata metadata) {

    /**
     * Compact constructor validating required fields.
     */
    public CatalogEntry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
