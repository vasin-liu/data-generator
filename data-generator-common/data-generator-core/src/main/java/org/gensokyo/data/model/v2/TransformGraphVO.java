/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L1 transform DAG inside a {@link org.gensokyo.data.model.v2.workflow.ComputeBlockVO}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@NoArgsConstructor
public class TransformGraphVO implements Serializable {
    /** Transform definitions keyed by id; nodes reference these via {@link TransformNodeVO#getTransformId()}. */
    private Map<String, TransformVO> transforms = new LinkedHashMap<>();
    private List<TransformNodeVO> nodes = new ArrayList<>();
    private List<TransformEdgeVO> edges = new ArrayList<>();
}
