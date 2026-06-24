/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

import org.gensokyo.data.datasource.api.CatalogEntry;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of YAML bootstrap catalog entries registered at application startup (D-25, D-26).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
@Component
public class CatalogBootstrapRegistry {

    private final ConcurrentHashMap<String, CatalogEntry> entries = new ConcurrentHashMap<>();

    /**
     * Registers or replaces a bootstrap catalog entry keyed by connection name.
     *
     * @param entry bootstrap metadata entry (must have {@code BOOTSTRAP} source)
     */
    public void register(CatalogEntry entry) {
        Objects.requireNonNull(entry, "entry");
        entries.put(entry.name(), entry);
    }

    /**
     * @param name connection name
     * @return bootstrap entry when registered at startup
     */
    public Optional<CatalogEntry> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(name));
    }

    /**
     * @return immutable snapshot of bootstrap entries
     */
    public Collection<CatalogEntry> entries() {
        return List.copyOf(entries.values());
    }

    /**
     * @param name connection name
     * @return true when the name is registered as a bootstrap entry
     */
    public boolean contains(String name) {
        return find(name).isPresent();
    }
}
