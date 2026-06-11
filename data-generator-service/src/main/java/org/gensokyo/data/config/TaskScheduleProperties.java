/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Scheduler settings for cron-driven template runs (Phase B schedule hook).
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = TaskScheduleProperties.PREFIX)
public class TaskScheduleProperties {

    /** Configuration prefix for task schedule properties. */
    public static final String PREFIX = DataGeneratorProperties.PREFIX + ".schedule";

    /**
     * When {@code true}, {@link org.gensokyo.data.task.TaskSchedulePoller} evaluates due rows.
     */
    private boolean enabled = false;

    /**
     * Fixed-delay polling interval in milliseconds for due schedule rows.
     */
    private long pollDelayMs = 60_000L;
}
