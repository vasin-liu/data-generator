/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Persistent queue row for distributed coordinator/worker execution (Phase C2).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Getter
@Setter
@Entity
@Table(name = "distributed_job")
public class DistributedJobPO implements Serializable {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "task_execution_id")
    private Long taskExecutionId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "worker_id", length = 128)
    private String workerId;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Column(name = "leased_at")
    private Instant leasedAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "payload_json", columnDefinition = "CLOB")
    private String payloadJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

