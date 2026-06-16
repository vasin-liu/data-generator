/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import jakarta.persistence.LockModeType;
import org.gensokyo.data.model.po.AiRateLimitStatePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence for distributed AI rate-limit buckets.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public interface AiRateLimitStateRepository extends JpaRepository<AiRateLimitStatePO, String> {

    /**
     * Loads a bucket row with a pessimistic write lock for coordinated acquire.
     *
     * @param limiterKey throttle bucket key
     * @return locked row when present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AiRateLimitStatePO s where s.limiterKey = :limiterKey")
    Optional<AiRateLimitStatePO> findLockedByLimiterKey(@Param("limiterKey") String limiterKey);
}
