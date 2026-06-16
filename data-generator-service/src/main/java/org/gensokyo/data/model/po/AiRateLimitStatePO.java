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
 * Shared AI rate-limit bucket persisted for multi-JVM coordination.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@Getter
@Setter
@Entity
@Table(name = "ai_rate_limit_state")
public class AiRateLimitStatePO implements Serializable {

    @Id
    @Column(name = "limiter_key", length = 256, nullable = false)
    private String limiterKey;

    @Column(name = "last_call_ms", nullable = false)
    private long lastCallMs;

    /**
     * JSON array of rolling-window call timestamps (epoch millis).
     */
    @Column(name = "window_timestamps_json", length = 4096)
    private String windowTimestampsJson;
}
