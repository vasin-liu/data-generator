/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2.workflow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-contained source → transform → sink unit executed inside a workflow.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@NoArgsConstructor
public class ComputeBlockVO implements Serializable {
    private String id;
    private Map<String, SourceVO> sources = new LinkedHashMap<>();
    private TransformGraphVO transformGraph;
    private List<TransformVO> transformers = new ArrayList<>();
    private List<WriteStageVO> sinks = new ArrayList<>();
    private String sharedScopeId;
}
