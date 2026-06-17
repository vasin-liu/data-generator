/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import jakarta.persistence.LockModeType;
import org.gensokyo.data.model.po.AiQuotaScopeDailyUsageId;
import org.gensokyo.data.model.po.AiQuotaScopeDailyUsagePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence for provider/template scoped AI daily quota counters.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public interface AiQuotaScopeDailyUsageRepository extends JpaRepository<AiQuotaScopeDailyUsagePO, AiQuotaScopeDailyUsageId> {

    /**
     * Loads a scoped UTC day row with a pessimistic write lock for quota reservation.
     *
     * @param usageDate UTC day key
     * @param scopeKey  scoped bucket key
     * @return locked row when present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from AiQuotaScopeDailyUsagePO q where q.id.usageDate = :usageDate and q.id.scopeKey = :scopeKey")
    Optional<AiQuotaScopeDailyUsagePO> findLockedByUsageDateAndScopeKey(
            @Param("usageDate") String usageDate,
            @Param("scopeKey") String scopeKey);
}
