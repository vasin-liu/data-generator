/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
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

/**
 * V2 template definition graph for Calcite-backed runs.
 * <p>
 * Optional {@link #metadata} may carry cross-cutting keys; {@code pipelineRef} is reserved for Phase D
 * pipeline orchestration.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
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
    private Map<String, Object> metadata;
}
