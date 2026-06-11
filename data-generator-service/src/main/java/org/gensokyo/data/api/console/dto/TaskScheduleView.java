/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.time.Instant;

/**
 * Console view of a cron-driven template schedule.
 *
 * @param id               schedule row id
 * @param templateId       template id
 * @param cronExpression   Spring cron expression
 * @param enabled          whether the schedule is active
 * @param description      optional label
 * @param lastTriggeredAt  last successful poll trigger time
 * @param lastInstanceId   last started task instance id when available
 * @param nextTriggerAt    next planned trigger time
 * @author Gensokyo
 * @since 2026-06-01
 */
public record TaskScheduleView(
        Long id,
        Long templateId,
        String cronExpression,
        boolean enabled,
        String description,
        Instant lastTriggeredAt,
        Long lastInstanceId,
        Instant nextTriggerAt) {
}
