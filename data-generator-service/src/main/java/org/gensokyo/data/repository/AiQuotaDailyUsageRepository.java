/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import jakarta.persistence.LockModeType;
import org.gensokyo.data.model.po.AiQuotaDailyUsagePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence for platform AI daily quota counters.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public interface AiQuotaDailyUsageRepository extends JpaRepository<AiQuotaDailyUsagePO, String> {

    /**
     * Loads a UTC day row with a pessimistic write lock for quota reservation.
     *
     * @param usageDate day key {@code yyyy-MM-dd}
     * @return locked row when present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from AiQuotaDailyUsagePO q where q.usageDate = :usageDate")
    Optional<AiQuotaDailyUsagePO> findLockedByUsageDate(@Param("usageDate") String usageDate);
}
