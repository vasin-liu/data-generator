/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2.workflow;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

/**
 * Workflow step that waits for a fixed duration, until a timestamp, or until a condition is met.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@AutoService(WorkflowStepVO.class)
@JsonSubType("PAUSE")
public class PauseStepVO extends WorkflowStepVO {

    /**
     * Creates a pause step with {@code type} set to {@code pause}.
     */
    public PauseStepVO() {
        setType("pause");
    }

    /** Fixed wait duration in milliseconds. */
    private Long durationMs;

    /** ISO-8601 instant to wait until; ignored when {@link #durationMs} is set. */
    private String until;

    /** SpEL condition evaluated until true; used when duration and until are unset. */
    private String condition;
}
