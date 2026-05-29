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
 * Workflow step that opens, reads, writes, or closes a named shared scope map.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@AutoService(WorkflowStepVO.class)
@JsonSubType("SHARED_SCOPE")
public class SharedScopeStepVO extends WorkflowStepVO {

    /**
     * Creates a shared-scope step with {@code type} set to {@code sharedScope}.
     */
    public SharedScopeStepVO() {
        setType("shared_scope");
    }

    /** Shared scope identifier referenced across workflow steps and compute blocks. */
    private String scopeId;

    /** Lifecycle action: {@code open}, {@code read}, {@code write}, or {@code close}. */
    private String action;

    /** Scope entries written when {@link #action} is {@code write}. */
    private Map<String, Object> entries = new LinkedHashMap<>();
}
