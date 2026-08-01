/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.model.po.GeoAssetPO;

import java.util.UUID;

/**
 * Post-upload response for a geo asset registration (D-10).
 *
 * @param id           assigned asset UUID
 * @param name         display name
 * @param featureCount parsed feature count
 * @param minLon       bbox minimum longitude
 * @param minLat       bbox minimum latitude
 * @param maxLon       bbox maximum longitude
 * @param maxLat       bbox maximum latitude
 * @author Gensokyo
 * @since 2026-08-01
 */
public record GeoAssetUploadView(
        UUID id,
        String name,
        int featureCount,
        double minLon,
        double minLat,
        double maxLon,
        double maxLat) {

    /**
     * Maps a persisted row to the upload acknowledgement view.
     *
     * @param row saved entity
     * @return upload view without GeoJSON body
     */
    public static GeoAssetUploadView from(GeoAssetPO row) {
        return new GeoAssetUploadView(
                row.getId(),
                row.getName(),
                row.getFeatureCount(),
                row.getMinLon(),
                row.getMinLat(),
                row.getMaxLon(),
                row.getMaxLat());
    }
}
