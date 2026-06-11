/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.DistributedJobPO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence and lease updates for distributed execution queue rows.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
public interface DistributedJobRepository extends JpaRepository<DistributedJobPO, Long> {

    /**
     * Finds earliest queue candidates that are either not leased yet or have expired leases.
     *
     * @param now      lease expiry threshold
     * @param pageable page request (typically size 1)
     * @return candidate queue rows
     */
    @Query("""
            select j
            from DistributedJobPO j
            where j.status = 'QUEUED'
               or (j.status in ('LEASED', 'RUNNING')
                   and j.leaseUntil is not null
                   and j.leaseUntil < :now)
            order by j.queuedAt asc
            """)
    List<DistributedJobPO> findLeaseCandidates(@Param("now") Instant now, Pageable pageable);

    /**
     * Attempts to acquire a lease for one candidate job.
     *
     * @param id         job id
     * @param workerId   worker identity
     * @param now        current timestamp
     * @param leaseUntil lease expiry timestamp
     * @return updated row count (1 when lease succeeded)
     */
    @Modifying
    @Query("""
            update DistributedJobPO j
            set j.status = 'LEASED',
                j.workerId = :workerId,
                j.leasedAt = :now,
                j.lastHeartbeatAt = :now,
                j.leaseUntil = :leaseUntil,
                j.attempts = j.attempts + 1,
                j.updatedAt = :now
            where j.id = :id
              and (j.status = 'QUEUED'
                   or (j.status in ('LEASED', 'RUNNING')
                       and j.leaseUntil is not null
                       and j.leaseUntil < :now))
            """)
    int tryAcquireLease(
            @Param("id") Long id,
            @Param("workerId") String workerId,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil);

    /**
     * Updates worker heartbeat and extends lease for an owned row.
     *
     * @param id         job id
     * @param workerId   worker identity
     * @param now        current timestamp
     * @param leaseUntil new lease expiry
     * @return updated row count
     */
    @Modifying
    @Query("""
            update DistributedJobPO j
            set j.lastHeartbeatAt = :now,
                j.leaseUntil = :leaseUntil,
                j.updatedAt = :now
            where j.id = :id
              and j.workerId = :workerId
              and (j.status = 'LEASED' or j.status = 'RUNNING')
            """)
    int touchHeartbeat(
            @Param("id") Long id,
            @Param("workerId") String workerId,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil);

    /**
     * @param id job id
     * @return queue row when present
     */
    Optional<DistributedJobPO> findById(Long id);

    /**
     * @param instanceId run instance id
     * @return latest queue row for the instance when present
     */
    Optional<DistributedJobPO> findFirstByInstanceIdOrderByQueuedAtDesc(Long instanceId);

    /**
     * @param status queue status
     * @return row count
     */
    long countByStatus(String status);

    /**
     * Aggregates queue depth grouped by status.
     *
     * @return status/count pairs
     */
    @Query("select j.status, count(j) from DistributedJobPO j group by j.status")
    List<Object[]> countGroupedByStatus();

    /**
     * Counts active leased/running rows per worker identity.
     *
     * @return workerId/count pairs
     */
    @Query("""
            select j.workerId, count(j)
            from DistributedJobPO j
            where j.workerId is not null
              and j.status in ('LEASED', 'RUNNING')
            group by j.workerId
            """)
    List<Object[]> countActiveJobsByWorker();
}

