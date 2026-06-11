/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.task.DistributedJobLease;
import org.gensokyo.data.task.DistributedJobService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Worker-facing REST contract for distributed job lease + heartbeat + completion (Phase C2).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@RestController
@RequestMapping("/task/distributed/jobs")
@Validated
@RequiredArgsConstructor
public class DistributedJobController {

    private final DistributedJobService distributedJobService;

    /**
     * Enqueues one distributed queue row for a known run instance.
     *
     * @param instanceId      run instance id
     * @param taskExecutionId optional execution row id
     * @param templateId      optional template id
     * @param payloadJson     optional opaque payload
     * @return queue row id
     */
    @PostMapping("/enqueue/{instanceId}")
    public R<Long> enqueue(
            @NotNull @PathVariable Long instanceId,
            @RequestParam(required = false) Long taskExecutionId,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String payloadJson) {
        return R.ok(distributedJobService.enqueue(taskExecutionId, templateId, instanceId, payloadJson));
    }

    /**
     * Claims one available queue row with a lease.
     *
     * @param workerId     worker identity
     * @param leaseSeconds lease ttl (seconds)
     * @return lease payload when available
     */
    @PostMapping("/lease/next")
    public R<DistributedJobLease> leaseNext(
            @NotBlank @RequestParam String workerId,
            @RequestParam(defaultValue = "30") int leaseSeconds) {
        Optional<DistributedJobLease> lease = distributedJobService.leaseNext(workerId, leaseSeconds);
        return R.ok(lease.orElse(null));
    }

    /**
     * Extends lease heartbeat for an owned row.
     *
     * @param jobId        queue row id
     * @param workerId     worker identity
     * @param leaseSeconds lease ttl (seconds)
     * @return acknowledgement
     */
    @PostMapping("/{jobId}/heartbeat")
    public R<String> heartbeat(
            @NotNull @PathVariable Long jobId,
            @NotBlank @RequestParam String workerId,
            @RequestParam(defaultValue = "30") int leaseSeconds) {
        distributedJobService.heartbeat(jobId, workerId, leaseSeconds);
        return R.ok("heartbeat accepted");
    }

    /**
     * Marks a leased row as worker-running.
     *
     * @param jobId    queue row id
     * @param workerId worker identity
     * @return acknowledgement
     */
    @PostMapping("/{jobId}/running")
    public R<String> markRunning(
            @NotNull @PathVariable Long jobId,
            @NotBlank @RequestParam String workerId) {
        distributedJobService.markRunning(jobId, workerId);
        return R.ok("running");
    }

    /**
     * Marks a leased/running row successful.
     *
     * @param jobId    queue row id
     * @param workerId worker identity
     * @return acknowledgement
     */
    @PostMapping("/{jobId}/success")
    public R<String> markSuccess(
            @NotNull @PathVariable Long jobId,
            @NotBlank @RequestParam String workerId) {
        distributedJobService.markSuccess(jobId, workerId);
        return R.ok("success");
    }

    /**
     * Marks a leased/running row cancelled.
     *
     * @param jobId    queue row id
     * @param workerId worker identity
     * @return acknowledgement
     */
    @PostMapping("/{jobId}/cancelled")
    public R<String> markCancelled(
            @NotNull @PathVariable Long jobId,
            @NotBlank @RequestParam String workerId) {
        distributedJobService.markCancelled(jobId, workerId);
        return R.ok("cancelled");
    }

    /**
     * Marks a leased/running row failed.
     *
     * @param jobId        queue row id
     * @param workerId     worker identity
     * @param errorMessage optional failure detail
     * @return acknowledgement
     */
    @PostMapping("/{jobId}/failed")
    public R<String> markFailed(
            @NotNull @PathVariable Long jobId,
            @NotBlank @RequestParam String workerId,
            @RequestParam(required = false) String errorMessage) {
        distributedJobService.markFailed(jobId, workerId, errorMessage);
        return R.ok("failed");
    }
}

