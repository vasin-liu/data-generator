package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;

import java.io.Serializable;
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
    private WriteStageVO sink;
    private List<WriteStageVO> sinks;
    private SinkExecutionPolicyVO sinkExecutionPolicy;
}
