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

/**
 * Platform AI usage counters for a single UTC calendar day.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@Getter
@Setter
@Entity
@Table(name = "ai_quota_daily_usage")
public class AiQuotaDailyUsagePO implements Serializable {

    /** UTC day key in {@code yyyy-MM-dd} form. */
    @Id
    @Column(name = "usage_date", length = 10, nullable = false)
    private String usageDate;

    @Column(name = "call_count", nullable = false)
    private long callCount;

    @Column(name = "prompt_tokens", nullable = false)
    private long promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private long completionTokens;

    @Column(name = "estimated_cost_usd", nullable = false)
    private double estimatedCostUsd;
}
