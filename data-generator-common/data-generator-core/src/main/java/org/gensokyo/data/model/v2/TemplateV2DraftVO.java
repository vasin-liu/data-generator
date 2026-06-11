package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.model.v2.workflow.WorkflowSpecVO;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TemplateV2DraftVO implements Serializable {
    private Long id;
    private Long instanceId;
    private String name;
    private GeneratorVO generator;
    private Map<String, SourceVO> sources;
    private TransformVO transform;
    private List<TransformVO> transformers;
    private List<TransformerCapabilityVO> transformerCapabilities;
    private WriteStageVO sink;
    private List<WriteStageVO> sinks;
    private ExecutionPolicyVO executionPolicy;
    private SinkExecutionPolicyVO sinkExecutionPolicy;
    /** Optional L2 workflow definition. */
    private WorkflowSpecVO workflow;
    /** Compute blocks referenced by workflow invoke steps. */
    private List<ComputeBlockVO> computeBlocks = new ArrayList<>();
    /** Operator-defined category for catalog grouping. */
    private String category;
    /** Operator-defined tags for search and filtering. */
    private List<String> tags = new ArrayList<>();
}
