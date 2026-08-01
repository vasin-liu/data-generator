/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

/**
 * Resolves durable GeoJSON assets stored in the metadata DB at pipeline runtime.
 *
 * <p>Callers must pass a bare UUID string (no {@code asset:} prefix). Template mappers and
 * {@code GeoResourceResolver} normalize {@code asset:{uuid}} references before invoking this contract.
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
public interface GeoAssetResolver {

    /**
     * Loads the authoritative UTF-8 GeoJSON body for a registered asset.
     *
     * @param assetId bare UUID string of the geo asset (non-blank)
     * @return validated GeoJSON text as stored at upload
     * @throws IllegalArgumentException when the asset id is unknown; message includes the id
     */
    String resolveUtf8(String assetId);
}
