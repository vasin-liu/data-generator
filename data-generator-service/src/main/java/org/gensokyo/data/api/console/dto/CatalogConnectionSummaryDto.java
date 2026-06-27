/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.ConnectionHealthStatus;

import java.time.Instant;

/**
 * Catalog list row for console datasource overview (D-26).
 *
 * @param name            connection name
 * @param kind            JDBC, KAFKA, or ELASTICSEARCH
 * @param source          BOOTSTRAP or MANAGED
 * @param healthStatus    HEALTHY or DEGRADED
 * @param lastReloadAt    timestamp of the most recent reload attempt
 * @param degradedReason  operator-facing failure summary when DEGRADED
 * @param version         catalog generation for snapshot pinning
 * @param updatedAt       last config mutation when known
 */
public record CatalogConnectionSummaryDto(
        String name,
        String kind,
        String source,
        String healthStatus,
        Instant lastReloadAt,
        String degradedReason,
        long version,
        Instant updatedAt) {

    /**
     * @param entry catalog entry
     * @return console DTO
     */
    public static CatalogConnectionSummaryDto from(CatalogEntry entry) {
        return new CatalogConnectionSummaryDto(
                entry.name(),
                entry.kind().name(),
                entry.source().name(),
                entry.healthStatus().name(),
                entry.lastReloadAt(),
                entry.degradedReason(),
                entry.version(),
                entry.updatedAt());
    }

    /**
     * @return {@code true} when the entry is in DEGRADED health state
     */
    public boolean degraded() {
        return ConnectionHealthStatus.DEGRADED.name().equals(healthStatus);
    }
}
