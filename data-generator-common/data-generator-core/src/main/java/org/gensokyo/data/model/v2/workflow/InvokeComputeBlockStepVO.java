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
 * Workflow step that executes a named {@link ComputeBlockVO} by id.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@AutoService(WorkflowStepVO.class)
@JsonSubType("INVOKE_COMPUTE_BLOCK")
public class InvokeComputeBlockStepVO extends WorkflowStepVO {

    /**
     * Creates an invoke step with {@code type} set to {@code invokeComputeBlock}.
     */
    public InvokeComputeBlockStepVO() {
        setType("invoke_compute_block");
    }

    /** Identifier of the compute block to run. */
    private String computeBlockId;
}
