/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import java.util.Objects;

/**
 * Shared helpers for actionable catalog resolve errors (D-07).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public final class CatalogResolveSupport {

    private CatalogResolveSupport() {
    }

    /**
     * Builds an {@link IllegalArgumentException} for unknown or mismatched catalog entries.
     *
     * @param name connection name requested
     * @param kind connection kind requested
     * @param hint additional operator guidance (may be blank)
     * @return exception with name, kind, and hint in the message
     */
    public static IllegalArgumentException unknownConnection(String name, ConnectionKind kind, String hint) {
        Objects.requireNonNull(kind, "kind");
        String safeName = name == null ? "<null>" : name;
        String suffix = hint == null || hint.isBlank() ? "" : " Hint: " + hint.trim();
        return new IllegalArgumentException(
                "Unknown connection name='" + safeName + "', kind=" + kind + "." + suffix);
    }
}
