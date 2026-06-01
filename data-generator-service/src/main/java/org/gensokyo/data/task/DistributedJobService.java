/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.po.DistributedJobPO;
import org.gensokyo.data.repository.DistributedJobRepository;
import org.gensokyo.data.util.RandomKit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Queue + lease lifecycle for distributed coordinator/worker execution (Phase C2).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Service
@RequiredArgsConstructor
public class DistributedJobService {

    private final DistributedJobRepository repository;

    /**
     * Enqueues a distributed execution row.
     *
     * @param taskExecutionId optional task execution row id
     * @param templateId      optional template id
     * @param instanceId      run instance id
     * @param payloadJson     optional execution payload
     * @return queue row id
     */
    @Transactional
    public Long enqueue(Long taskExecutionId, Long templateId, Long instanceId, String payloadJson) {
        Instant now = Instant.now();
        DistributedJobPO row = new DistributedJobPO();
        row.setId(RandomKit.snowFlake().nextId());
        row.setTaskExecutionId(taskExecutionId);
        row.setTemplateId(templateId);
        row.setInstanceId(instanceId);
        row.setStatus(DistributedJobStatus.QUEUED.name());
        row.setQueuedAt(now);
        row.setPayloadJson(payloadJson);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        repository.saveAndFlush(row);
        return row.getId();
    }

    /**
     * Claims one available queue row and acquires a lease.
     *
     * @param workerId     worker identity
     * @param leaseSeconds lease ttl seconds
     * @return lease view when successful
     */
    @Transactional
    public Optional<DistributedJobLease> leaseNext(String workerId, int leaseSeconds) {
        validateWorker(workerId);
        if (leaseSeconds <= 0) {
            throw new IllegalArgumentException("leaseSeconds must be positive");
        }
        Instant now = Instant.now();
        Instant leaseUntil = now.plusSeconds(leaseSeconds);
        // Iterate candidates to handle races where one candidate was leased by another worker.
        for (DistributedJobPO candidate : repository.findLeaseCandidates(now, PageRequest.of(0, 8))) {
            int updated = repository.tryAcquireLease(candidate.getId(), workerId, now, leaseUntil);
            if (updated == 1) {
                DistributedJobPO leased = repository.findById(candidate.getId())
                        .orElseThrow(() -> new IllegalStateException("Leased distributed job not found: " + candidate.getId()));
                return Optional.of(toLease(leased));
            }
        }
        return Optional.empty();
    }

    /**
     * Extends lease and heartbeat timestamp for an owned row.
     *
     * @param jobId        queue row id
     * @param workerId     worker identity
     * @param leaseSeconds lease ttl seconds
     */
    @Transactional
    public void heartbeat(Long jobId, String workerId, int leaseSeconds) {
        validateWorker(workerId);
        if (leaseSeconds <= 0) {
            throw new IllegalArgumentException("leaseSeconds must be positive");
        }
        Instant now = Instant.now();
        int updated = repository.touchHeartbeat(jobId, workerId, now, now.plusSeconds(leaseSeconds));
        if (updated != 1) {
            throw new IllegalArgumentException("Unable to heartbeat distributed job: " + jobId);
        }
    }

    /**
     * Marks a leased row as worker-running.
     *
     * @param jobId    queue row id
     * @param workerId worker identity
     */
    @Transactional
    public void markRunning(Long jobId, String workerId) {
        validateWorker(workerId);
        DistributedJobPO row = requireOwned(jobId, workerId);
        if (!DistributedJobStatus.LEASED.name().equals(row.getStatus())
                && !DistributedJobStatus.RUNNING.name().equals(row.getStatus())) {
            throw new IllegalArgumentException("Distributed job is not leased: " + jobId);
        }
        Instant now = Instant.now();
        row.setStatus(DistributedJobStatus.RUNNING.name());
        if (row.getStartedAt() == null) {
            row.setStartedAt(now);
        }
        row.setUpdatedAt(now);
        repository.saveAndFlush(row);
    }

    /**
     * Marks a leased/running row successful.
     *
     * @param jobId    queue row id
     * @param workerId worker identity
     */
    @Transactional
    public void markSuccess(Long jobId, String workerId) {
        markTerminal(jobId, workerId, DistributedJobStatus.SUCCESS, null);
    }

    /**
     * Marks a leased/running row failed with optional error detail.
     *
     * @param jobId        queue row id
     * @param workerId     worker identity
     * @param errorMessage failure detail
     */
    @Transactional
    public void markFailed(Long jobId, String workerId, String errorMessage) {
        markTerminal(jobId, workerId, DistributedJobStatus.FAILED, errorMessage);
    }

    private void markTerminal(Long jobId, String workerId, DistributedJobStatus status, String errorMessage) {
        validateWorker(workerId);
        DistributedJobPO row = requireOwned(jobId, workerId);
        if (!DistributedJobStatus.LEASED.name().equals(row.getStatus())
                && !DistributedJobStatus.RUNNING.name().equals(row.getStatus())) {
            throw new IllegalArgumentException("Distributed job is not active: " + jobId);
        }
        Instant now = Instant.now();
        row.setStatus(status.name());
        row.setFinishedAt(now);
        row.setLeaseUntil(now);
        row.setUpdatedAt(now);
        row.setErrorMessage(trimError(errorMessage));
        repository.saveAndFlush(row);
    }

    private static String trimError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        if (errorMessage.length() <= 4000) {
            return errorMessage;
        }
        return errorMessage.substring(0, 4000);
    }

    private DistributedJobPO requireOwned(Long jobId, String workerId) {
        DistributedJobPO row = repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown distributed job: " + jobId));
        if (!workerId.equals(row.getWorkerId())) {
            throw new IllegalArgumentException("Distributed job lease owned by another worker: " + jobId);
        }
        return row;
    }

    private static DistributedJobLease toLease(DistributedJobPO row) {
        return new DistributedJobLease(
                row.getId(),
                row.getInstanceId(),
                row.getTemplateId(),
                row.getLeaseUntil(),
                row.getPayloadJson());
    }

    private static void validateWorker(String workerId) {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }
    }
}

