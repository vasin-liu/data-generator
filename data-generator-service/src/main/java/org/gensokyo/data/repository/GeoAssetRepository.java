/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.GeoAssetPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence access for {@link GeoAssetPO} rows.
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
public interface GeoAssetRepository extends JpaRepository<GeoAssetPO, UUID> {

    /**
     * Lists assets newest-first for the console registry page.
     *
     * @return all rows ordered by {@code updated_at} descending
     */
    List<GeoAssetPO> findAllByOrderByUpdatedAtDesc();
}
