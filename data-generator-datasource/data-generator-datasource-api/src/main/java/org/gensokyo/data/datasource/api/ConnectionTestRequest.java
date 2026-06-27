/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

import java.util.Map;
import java.util.Objects;

/**
 * Unified connectivity test input for an existing catalog entry or an unsaved draft config (D-18).
 * Draft payloads must use {@code secretRef} fields instead of plaintext secret values.
 *
 * @param kind         connection kind under test
 * @param name         catalog entry name when testing a persisted connection; {@code null} for drafts
 * @param draftPayload unsaved kind-specific config map when testing before save; {@code null} for named entries
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public record ConnectionTestRequest(
        ConnectionKind kind,
        String name,
        Map<String, Object> draftPayload) {

    /**
     * Compact constructor validating mutually exclusive name vs draft payload modes.
     */
    public ConnectionTestRequest {
        Objects.requireNonNull(kind, "kind");
        boolean hasName = name != null && !name.isBlank();
        boolean hasDraft = draftPayload != null && !draftPayload.isEmpty();
        if (hasName == hasDraft) {
            throw new IllegalArgumentException(
                    "Provide either a non-blank name for an existing entry or a non-empty draftPayload, not both");
        }
        if (hasName) {
            name = name.trim();
        }
        if (hasDraft) {
            draftPayload = Map.copyOf(draftPayload);
        }
    }

    /**
     * Tests connectivity for a catalog entry already registered under {@code name}.
     *
     * @param kind connection kind
     * @param name catalog entry name
     * @return request targeting an existing entry
     */
    public static ConnectionTestRequest forExisting(ConnectionKind kind, String name) {
        return new ConnectionTestRequest(kind, name, null);
    }

    /**
     * Tests connectivity for an unsaved draft configuration map (console create/edit flow).
     *
     * @param kind    connection kind
     * @param payload kind-specific config parameters without resolved runtime handles
     * @return request targeting a draft payload
     */
    public static ConnectionTestRequest forDraft(ConnectionKind kind, Map<String, Object> payload) {
        return new ConnectionTestRequest(kind, null, payload);
    }

    /**
     * @return {@code true} when this request targets a named catalog entry
     */
    public boolean isExistingEntry() {
        return name != null;
    }
}
