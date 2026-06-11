/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create or update body for a template run schedule.
 *
 * @param templateId     template id
 * @param cronExpression Spring six-field cron expression
 * @param enabled        whether the schedule is active
 * @param description    optional label
 * @author Gensokyo
 * @since 2026-06-01
 */
public record TaskScheduleUpsertRequest(
        @NotNull Long templateId,
        @NotBlank String cronExpression,
        Boolean enabled,
        String description) {
}
