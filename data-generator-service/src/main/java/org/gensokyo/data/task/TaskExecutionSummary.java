/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.gensokyo.data.model.v2.RunReportVO;

import java.io.Serializable;
import java.time.Instant;

/**
 * API view of a task execution row.
 *
 * @param id              execution row id
 * @param templateId      template id
 * @param templateName    template name
 * @param instanceId      run instance id
 * @param definitionKind  V1 or V2
 * @param triggerType     MANUAL or SCHEDULED
 * @param scheduleId      originating schedule id for SCHEDULED runs
 * @param status          lifecycle status
 * @param queuedAt        queued timestamp
 * @param startedAt       started timestamp
 * @param finishedAt      finished timestamp
 * @param rowCount        rows processed when known
 * @param errorMessage    failure message
 * @param metricsJson     serialized V2 metrics
 * @param report          structured V2 run report when available
 * @param pauseReason     operator-visible reason when status is {@code PAUSED}
 */
public record TaskExecutionSummary(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        @JsonSerialize(using = ToStringSerializer.class) Long templateId,
        String templateName,
        @JsonSerialize(using = ToStringSerializer.class) Long instanceId,
        String definitionKind,
        String triggerType,
        @JsonSerialize(using = ToStringSerializer.class) Long scheduleId,
        String status,
        Instant queuedAt,
        Instant startedAt,
        Instant finishedAt,
        Long rowCount,
        String errorMessage,
        String metricsJson,
        RunReportVO report,
        String pauseReason) implements Serializable {
}
