/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.model.v2.TransformEdgeVO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformNodeVO;
import org.gensokyo.data.model.v2.TransformVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Truncates an L1 transform DAG for staged in-memory preview.
 *
 * @author Gensokyo
 * @since 2026-06-10
 */
public final class TransformGraphPreviewSupport {

    private TransformGraphPreviewSupport() {
    }

    /**
     * Returns a subgraph containing every node up to and including {@code throughNodeId} in topological order.
     *
     * @param graph          full transform DAG
     * @param throughNodeId  last node to execute in preview
     * @return truncated graph safe to pass to {@link TransformDagExecutor}
     * @throws IllegalArgumentException when the node id is missing from the graph
     * @throws TransformDagValidationException when the source graph is invalid
     */
    public static TransformGraphVO truncateThroughNode(TransformGraphVO graph, String throughNodeId) {
        if (graph == null) {
            throw new IllegalArgumentException("transformGraph is required for DAG staged preview");
        }
        if (throughNodeId == null || throughNodeId.isBlank()) {
            throw new IllegalArgumentException("throughTransformNodeId must not be blank");
        }
        TransformDagExecutor executor = new TransformDagExecutor();
        List<String> order = executor.topologicalSort(graph);
        int throughIndex = order.indexOf(throughNodeId);
        if (throughIndex < 0) {
            throw new IllegalArgumentException("throughTransformNodeId not found in transform DAG: " + throughNodeId);
        }
        Set<String> retained = new LinkedHashSet<>(order.subList(0, throughIndex + 1));

        TransformGraphVO truncated = new TransformGraphVO();
        List<TransformNodeVO> nodes = new ArrayList<>();
        Set<String> transformIds = new LinkedHashSet<>();
        for (TransformNodeVO node : graph.getNodes()) {
            if (node != null && retained.contains(node.getId())) {
                nodes.add(node);
                if (node.getTransformId() != null) {
                    transformIds.add(node.getTransformId());
                }
            }
        }
        truncated.setNodes(nodes);

        List<TransformEdgeVO> edges = new ArrayList<>();
        for (TransformEdgeVO edge : graph.getEdges()) {
            if (edge != null
                    && retained.contains(edge.getFromNodeId())
                    && retained.contains(edge.getToNodeId())) {
                edges.add(edge);
            }
        }
        truncated.setEdges(edges);

        Map<String, TransformVO> transforms = new LinkedHashMap<>();
        if (graph.getTransforms() != null) {
            for (String transformId : transformIds) {
                TransformVO transform = graph.getTransforms().get(transformId);
                if (transform != null) {
                    transforms.put(transformId, transform);
                }
            }
        }
        truncated.setTransforms(transforms);
        return truncated;
    }
}
