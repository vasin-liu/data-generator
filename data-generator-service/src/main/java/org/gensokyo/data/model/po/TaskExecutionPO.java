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
 * Persisted template run for operator job center (P3).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Getter
@Setter
@Entity
@Table(name = "task_execution")
public class TaskExecutionPO implements Serializable {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "template_name", length = 128)
    private String templateName;

    @Column(name = "instance_id", nullable = false, unique = true)
    private Long instanceId;

    @Column(name = "definition_kind", length = 8)
    private String definitionKind;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "metrics_json", columnDefinition = "CLOB")
    private String metricsJson;
}
