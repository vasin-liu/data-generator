/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.source.IteratorSourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TransformEdgeVO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformNodeVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for {@link TransformDagExecutor} and {@link ComputeBlockRunner} L1 DAG execution.
 */
class TransformDagExecutorTests {

    @Test
    void executesTwoSqlNodesMergedByEdge() {
        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("merge-block");
        block.setSources(Map.of("seed", numberSource(1, 5, 1)));

        SqlTransformVO filterTransform = sql("SELECT value FROM seed WHERE value >= 4");
        filterTransform.setName("filter-high");
        SqlTransformVO shiftTransform = sql("SELECT value, value + 10 AS shifted FROM input");
        shiftTransform.setName("shift-values");

        TransformGraphVO graph = new TransformGraphVO();
        graph.setTransforms(Map.of(
                "filter-high", filterTransform,
                "shift-values", shiftTransform));

        TransformNodeVO first = new TransformNodeVO();
        first.setId("n1");
        first.setTransformId("filter-high");
        first.setOutputAlias("filtered");

        TransformNodeVO second = new TransformNodeVO();
        second.setId("n2");
        second.setTransformId("shift-values");
        second.setOutputAlias("output");

        TransformEdgeVO edge = new TransformEdgeVO();
        edge.setFromNodeId("n1");
        edge.setFromPort("out");
        edge.setToNodeId("n2");
        edge.setToPort("in");

        graph.setNodes(List.of(first, second));
        graph.setEdges(List.of(edge));
        block.setTransformGraph(graph);
        block.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new ComputeBlockRunner().run(
                block,
                EffectiveExecutionPolicy.resolve(new ExecutionPolicyVO()),
                defaultRegistry());

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("4", result.getRows().get(0).getString("value"));
        Assertions.assertEquals("14", result.getRows().get(0).getString("shifted"));
        Assertions.assertEquals("5", result.getRows().get(1).getString("value"));
        Assertions.assertEquals("15", result.getRows().get(1).getString("shifted"));
    }

    @Test
    void rejectsCyclicTransformGraphWithCyclePath() {
        TransformGraphVO graph = cyclicGraph();

        TransformDagValidationException exception = Assertions.assertThrows(
                TransformDagValidationException.class,
                () -> new TransformDagExecutor().topologicalSort(graph));

        Assertions.assertFalse(exception.getCyclePath().isEmpty());
        Assertions.assertTrue(exception.getMessage().contains("cycle"));
    }

    @Test
    void linearTransformersFallbackWhenGraphAbsent() {
        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("linear-block");
        block.setSources(Map.of("seed", numberSource(1, 3, 1)));
        block.setTransformers(List.of(sql("SELECT value, value + 1 AS next_value FROM seed WHERE value >= 2")));

        TemplateV2RunResult result = new ComputeBlockRunner().run(
                block,
                EffectiveExecutionPolicy.resolve(new ExecutionPolicyVO()),
                defaultRegistry());

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("2", result.getRows().get(0).getString("value"));
        Assertions.assertEquals("3", result.getRows().get(0).getString("next_value"));
    }

    private static TransformGraphVO cyclicGraph() {
        SqlTransformVO passThrough = sql("SELECT value FROM seed");
        passThrough.setName("pass");

        TransformGraphVO graph = new TransformGraphVO();
        graph.setTransforms(Map.of("pass", passThrough));

        TransformNodeVO n1 = new TransformNodeVO();
        n1.setId("n1");
        n1.setTransformId("pass");
        TransformNodeVO n2 = new TransformNodeVO();
        n2.setId("n2");
        n2.setTransformId("pass");
        graph.setNodes(List.of(n1, n2));

        TransformEdgeVO e1 = new TransformEdgeVO();
        e1.setFromNodeId("n1");
        e1.setToNodeId("n2");
        TransformEdgeVO e2 = new TransformEdgeVO();
        e2.setFromNodeId("n2");
        e2.setToNodeId("n1");
        graph.setEdges(List.of(e1, e2));
        return graph;
    }

    private static IteratorSourceVO numberSource(long from, long to, int step) {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(from);
        iterator.setTo(to);
        iterator.setStep(step);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private static SqlTransformVO sql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

    private static WriteStageVO consoleSink() {
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
        return sink;
    }

    private static TemplateV2RuntimeRegistry defaultRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}
