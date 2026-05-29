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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Workflow step that emits structured log output and run-report metrics.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@AutoService(WorkflowStepVO.class)
@JsonSubType("LOG")
public class LogStepVO extends WorkflowStepVO {

    /**
     * Creates a log step with {@code type} set to {@code log}.
     */
    public LogStepVO() {
        setType("log");
    }

    /** Log level (for example {@code INFO} or {@code WARN}). */
    private String level;

    /** Human-readable message written to the run report. */
    private String message;

    /** Optional structured fields attached to the log entry. */
    private Map<String, Object> fields = new LinkedHashMap<>();
}
