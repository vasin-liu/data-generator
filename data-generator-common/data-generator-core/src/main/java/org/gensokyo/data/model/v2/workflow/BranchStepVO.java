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

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow step that routes execution based on a SpEL condition.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@AutoService(WorkflowStepVO.class)
@JsonSubType("BRANCH")
public class BranchStepVO extends WorkflowStepVO {

    /**
     * Creates a branch step with {@code type} set to {@code branch}.
     */
    public BranchStepVO() {
        setType("branch");
    }

    /** SpEL expression evaluated to choose the branch path. */
    private String condition;

    /** Nested steps executed when {@link #condition} is true. */
    private List<WorkflowStepVO> thenSteps = new ArrayList<>();

    /** Nested steps executed when {@link #condition} is false. */
    private List<WorkflowStepVO> elseSteps = new ArrayList<>();

    /** Optional compute block id executed instead of {@link #thenSteps}. */
    private String thenComputeBlockId;
}
