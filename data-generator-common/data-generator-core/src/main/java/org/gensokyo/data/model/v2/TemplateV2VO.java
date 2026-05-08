package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gensokyo.data.model.vo.generator.GeneratorVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class TemplateV2VO implements Serializable {
    private Long id;
    private Long instanceId;
    private String name;
    private GeneratorVO generator;
    private Map<String, SourceVO> sources = new LinkedHashMap<>();
    private List<TransformVO> transformers = new ArrayList<>();
    private List<TransformerCapabilityVO> transformerCapabilities = new ArrayList<>();
    private List<WriteStageVO> sinks = new ArrayList<>();
    private ExecutionPolicyVO executionPolicy;
    private SinkExecutionPolicyVO sinkExecutionPolicy;
}
