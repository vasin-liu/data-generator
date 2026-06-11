/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.TransformEdgeVO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformNodeVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.kit.character.StrKit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executes an L1 transform DAG inside a compute block using topological order.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class TransformDagExecutor {

    /**
     * Executes all nodes in the graph and returns the output of terminal sink nodes.
     *
     * @param graph         transform DAG definition
     * @param sourceContext execution context with materialized sources
     * @param registry      runtime registry for transform factories
     * @return combined output of terminal nodes (single sink) or the last sink in topo order
     * @throws TransformDagValidationException when the graph is invalid or cyclic
     */
    public CalciteRowTransformer.TransformResult execute(
            TransformGraphVO graph,
            CalciteExecutionContext sourceContext,
            TemplateV2RuntimeRegistry registry) {
        validateGraph(graph);
        List<String> order = topologicalSort(graph);

        Map<String, TransformNodeVO> nodesById = indexNodes(graph);
        Map<String, List<TransformEdgeVO>> incoming = incomingEdges(graph);
        Map<String, CalciteRowTransformer.TransformResult> nodeResults = new LinkedHashMap<>();

        for (String nodeId : order) {
            TransformNodeVO node = nodesById.get(nodeId);
            CalciteExecutionContext nodeContext = buildNodeContext(sourceContext, incoming.get(nodeId), nodeResults);
            TransformVO transform = graph.getTransforms().get(node.getTransformId());
            CalciteRowTransformer.TransformResult result = registry.applyTransform(transform, nodeContext);
            nodeResults.put(nodeId, result);
        }

        List<String> sinkNodeIds = sinkNodes(graph, order);
        if (sinkNodeIds.isEmpty()) {
            throw new TransformDagValidationException("Transform DAG produced no sink nodes");
        }
        // Single-chain graphs expose the last sink; multi-sink blocks return the last sink in topo order.
        String finalSinkId = sinkNodeIds.getLast();
        return nodeResults.get(finalSinkId);
    }

    /**
     * Returns node ids in topological execution order.
     *
     * @param graph transform DAG definition
     * @return ordered node ids
     * @throws TransformDagValidationException when the graph contains a cycle
     */
    public List<String> topologicalSort(TransformGraphVO graph) {
        validateGraph(graph);
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, List<String>> outgoing = new LinkedHashMap<>();
        for (TransformNodeVO node : graph.getNodes()) {
            indegree.put(node.getId(), 0);
            outgoing.put(node.getId(), new ArrayList<>());
        }
        for (TransformEdgeVO edge : graph.getEdges()) {
            indegree.merge(edge.getToNodeId(), 1, Integer::sum);
            outgoing.computeIfAbsent(edge.getFromNodeId(), ignored -> new ArrayList<>()).add(edge.getToNodeId());
        }

        Deque<String> ready = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String nodeId = ready.removeFirst();
            order.add(nodeId);
            for (String downstream : outgoing.getOrDefault(nodeId, List.of())) {
                int next = indegree.merge(downstream, -1, Integer::sum);
                if (next == 0) {
                    ready.addLast(downstream);
                }
            }
        }

        if (order.size() != graph.getNodes().size()) {
            List<String> cyclePath = findCyclePath(graph);
            throw new TransformDagValidationException(
                    "Transform DAG contains a cycle: " + String.join(" -> ", cyclePath),
                    cyclePath);
        }
        return order;
    }

    private static void validateGraph(TransformGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new TransformDagValidationException("Transform DAG must contain at least one node");
        }
        Set<String> nodeIds = new HashSet<>();
        for (TransformNodeVO node : graph.getNodes()) {
            if (node == null || StrKit.isBlank(node.getId())) {
                throw new TransformDagValidationException("Transform DAG node id must not be blank");
            }
            if (!nodeIds.add(node.getId())) {
                throw new TransformDagValidationException("Duplicate transform DAG node id: " + node.getId());
            }
            if (StrKit.isBlank(node.getTransformId())) {
                throw new TransformDagValidationException("Transform DAG node '" + node.getId() + "' must reference a transform id");
            }
            TransformVO transform = graph.getTransforms().get(node.getTransformId());
            if (transform == null) {
                throw new TransformDagValidationException("Transform DAG node '" + node.getId()
                        + "' references unknown transform id '" + node.getTransformId() + "'");
            }
        }
        for (TransformEdgeVO edge : graph.getEdges()) {
            if (edge == null) {
                throw new TransformDagValidationException("Transform DAG edge must not be null");
            }
            if (!nodeIds.contains(edge.getFromNodeId())) {
                throw new TransformDagValidationException("Transform DAG edge references unknown from node: " + edge.getFromNodeId());
            }
            if (!nodeIds.contains(edge.getToNodeId())) {
                throw new TransformDagValidationException("Transform DAG edge references unknown to node: " + edge.getToNodeId());
            }
        }
    }

    private static Map<String, TransformNodeVO> indexNodes(TransformGraphVO graph) {
        Map<String, TransformNodeVO> nodesById = new LinkedHashMap<>();
        for (TransformNodeVO node : graph.getNodes()) {
            nodesById.put(node.getId(), node);
        }
        return nodesById;
    }

    private static Map<String, List<TransformEdgeVO>> incomingEdges(TransformGraphVO graph) {
        Map<String, List<TransformEdgeVO>> incoming = new LinkedHashMap<>();
        for (TransformNodeVO node : graph.getNodes()) {
            incoming.put(node.getId(), new ArrayList<>());
        }
        for (TransformEdgeVO edge : graph.getEdges()) {
            incoming.computeIfAbsent(edge.getToNodeId(), ignored -> new ArrayList<>()).add(edge);
        }
        return incoming;
    }

    private static CalciteExecutionContext buildNodeContext(
            CalciteExecutionContext sourceContext,
            List<TransformEdgeVO> incomingEdges,
            Map<String, CalciteRowTransformer.TransformResult> nodeResults) {
        CalciteExecutionContext context = new CalciteExecutionContext();
        sourceContext.getSchemas().forEach(context::addSchema);
        sourceContext.getData().forEach((table, rows) ->
                context.addTable(table, sourceContext.getSchemas().get(table), rows));

        if (incomingEdges == null || incomingEdges.isEmpty()) {
            return context;
        }

        for (TransformEdgeVO edge : incomingEdges) {
            CalciteRowTransformer.TransformResult upstream = nodeResults.get(edge.getFromNodeId());
            if (upstream == null) {
                throw new IllegalStateException("Missing upstream transform result for node '" + edge.getFromNodeId() + "'");
            }
            String tableAlias = resolveInputAlias(edge);
            context.addTable(tableAlias, upstream.schema(), upstream.rows());
        }
        return context;
    }

    private static String resolveInputAlias(TransformEdgeVO edge) {
        if (StrKit.isBlank(edge.getToPort()) || "in".equalsIgnoreCase(edge.getToPort())) {
            return "input";
        }
        return edge.getToPort();
    }

    private static List<String> sinkNodes(TransformGraphVO graph, List<String> topologicalOrder) {
        Set<String> hasOutgoing = new LinkedHashSet<>();
        for (TransformEdgeVO edge : graph.getEdges()) {
            hasOutgoing.add(edge.getFromNodeId());
        }
        List<String> sinks = new ArrayList<>();
        for (String nodeId : topologicalOrder) {
            if (!hasOutgoing.contains(nodeId)) {
                sinks.add(nodeId);
            }
        }
        return sinks;
    }

    private static List<String> findCyclePath(TransformGraphVO graph) {
        Map<String, List<String>> outgoing = new LinkedHashMap<>();
        for (TransformNodeVO node : graph.getNodes()) {
            outgoing.put(node.getId(), new ArrayList<>());
        }
        for (TransformEdgeVO edge : graph.getEdges()) {
            outgoing.computeIfAbsent(edge.getFromNodeId(), ignored -> new ArrayList<>()).add(edge.getToNodeId());
        }

        Map<String, Integer> state = new HashMap<>();
        List<String> stack = new ArrayList<>();
        for (String nodeId : outgoing.keySet()) {
            List<String> cycle = dfsCycle(nodeId, outgoing, state, stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        return List.of("unknown");
    }

    private static List<String> dfsCycle(
            String nodeId,
            Map<String, List<String>> outgoing,
            Map<String, Integer> state,
            List<String> stack) {
        Integer visited = state.get(nodeId);
        if (visited != null && visited == 1) {
            int start = stack.indexOf(nodeId);
            List<String> cycle = new ArrayList<>(stack.subList(start, stack.size()));
            cycle.add(nodeId);
            return cycle;
        }
        if (visited != null && visited == 2) {
            return List.of();
        }
        state.put(nodeId, 1);
        stack.add(nodeId);
        for (String next : outgoing.getOrDefault(nodeId, List.of())) {
            List<String> cycle = dfsCycle(next, outgoing, state, stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        stack.remove(stack.size() - 1);
        state.put(nodeId, 2);
        return List.of();
    }
}
