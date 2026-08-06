/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.model.po.GeoAssetPO;

import java.time.Instant;
import java.util.UUID;

/**
 * List/detail view for a geo asset without the GeoJSON body (D-10).
 *
 * @param id               asset UUID
 * @param name             display name
 * @param featureCount     parsed feature count
 * @param minLon           bbox minimum longitude
 * @param minLat           bbox minimum latitude
 * @param maxLon           bbox maximum longitude
 * @param maxLat           bbox maximum latitude
 * @param geometrySummary  optional geometry-type summary
 * @param contentType      stored content type when present on {@link GeoAssetPO} (D-02)
 * @param uploadedBy       console actor
 * @param createdAt        upload timestamp
 * @param updatedAt        last update timestamp
 * @author Gensokyo
 * @since 2026-08-01
 */
public record GeoAssetSummaryView(
        UUID id,
        String name,
        int featureCount,
        double minLon,
        double minLat,
        double maxLon,
        double maxLat,
        String geometrySummary,
        String contentType,
        String uploadedBy,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Maps a persisted row to a summary view (never includes {@code geojsonClob}).
     *
     * @param row JPA entity
     * @return summary DTO
     */
    public static GeoAssetSummaryView from(GeoAssetPO row) {
        return new GeoAssetSummaryView(
                row.getId(),
                row.getName(),
                row.getFeatureCount(),
                row.getMinLon(),
                row.getMinLat(),
                row.getMaxLon(),
                row.getMaxLat(),
                row.getGeometrySummary(),
                row.getContentType(),
                row.getUploadedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
