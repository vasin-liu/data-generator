/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TransformEdgeVO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformNodeVO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TransformGraphPreviewSupport}.
 *
 * @author Gensokyo
 * @since 2026-06-10
 */
class TransformGraphPreviewSupportTests {

    @Test
    void truncatesGraphThroughRequestedNode() {
        TransformGraphVO graph = sampleGraph();

        TransformGraphVO truncated = TransformGraphPreviewSupport.truncateThroughNode(graph, "n1");

        assertThat(truncated.getNodes()).extracting(TransformNodeVO::getId).containsExactly("n1");
        assertThat(truncated.getEdges()).isEmpty();
        assertThat(truncated.getTransforms()).containsOnlyKeys("filter-high");
    }

    @Test
    void rejectsUnknownNodeId() {
        assertThatThrownBy(() -> TransformGraphPreviewSupport.truncateThroughNode(sampleGraph(), "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("throughTransformNodeId");
    }

    private static TransformGraphVO sampleGraph() {
        SqlTransformVO filter = new SqlTransformVO();
        filter.setName("filter-high");
        filter.setSql("SELECT value FROM seed WHERE value >= 4");

        SqlTransformVO shift = new SqlTransformVO();
        shift.setName("shift-values");
        shift.setSql("SELECT value, value + 10 AS shifted FROM input");

        Map<String, org.gensokyo.data.model.v2.TransformVO> transforms = new LinkedHashMap<>();
        transforms.put("filter-high", filter);
        transforms.put("shift-values", shift);

        TransformNodeVO n1 = new TransformNodeVO();
        n1.setId("n1");
        n1.setTransformId("filter-high");
        n1.setOutputAlias("filtered");

        TransformNodeVO n2 = new TransformNodeVO();
        n2.setId("n2");
        n2.setTransformId("shift-values");
        n2.setOutputAlias("output");

        TransformEdgeVO edge = new TransformEdgeVO();
        edge.setFromNodeId("n1");
        edge.setFromPort("out");
        edge.setToNodeId("n2");
        edge.setToPort("in");

        TransformGraphVO graph = new TransformGraphVO();
        graph.setTransforms(transforms);
        graph.setNodes(List.of(n1, n2));
        graph.setEdges(List.of(edge));
        return graph;
    }
}
