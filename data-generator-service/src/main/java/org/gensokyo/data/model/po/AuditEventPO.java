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
 * Append-only operator audit event (Phase B).
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@Entity
@Table(name = "audit_event")
public class AuditEventPO implements Serializable {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor", length = 128)
    private String actor;

    @Column(name = "action", length = 64, nullable = false)
    private String action;

    @Column(name = "resource_type", length = 32, nullable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 256)
    private String resourceId;

    @Column(name = "detail_json", columnDefinition = "CLOB")
    private String detailJson;
}
