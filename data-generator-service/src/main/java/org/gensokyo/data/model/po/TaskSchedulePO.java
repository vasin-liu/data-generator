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
 * Cron schedule row that triggers published template runs (Phase B schedule hook).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Getter
@Setter
@Entity
@Table(name = "task_schedule")
public class TaskSchedulePO implements Serializable {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "cron_expression", length = 128, nullable = false)
    private String cronExpression;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "last_instance_id")
    private Long lastInstanceId;

    @Column(name = "next_trigger_at")
    private Instant nextTriggerAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
