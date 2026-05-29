/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.JsTransformVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformEdgeVO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformNodeVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.v2.workflow.InvokeComputeBlockStepVO;
import org.gensokyo.data.model.v2.workflow.PauseStepVO;
import org.gensokyo.data.model.v2.workflow.WorkflowSpecVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Workflow, compute block, and transform DAG validation for {@link TemplateV2Validator}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
class TemplateV2WorkflowValidatorTests {

    @Test
    void validatesWorkflowTemplateWithComputeBlock() {
        TemplateV2VO template = workflowTemplateWithComputeBlock();

        Assertions.assertDoesNotThrow(() -> TemplateV2Validator.validate(template));
    }

    @Test
    void rejectsWorkflowWithTopLevelTransformersAndNoComputeBlocks() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("wf-conflict");
        template.setWorkflow(singlePauseWorkflow());
        template.setTransformers(List.of(sql("SELECT value FROM input")));

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TemplateV2Validator.validate(template));

        Assertions.assertTrue(ex.getMessage().contains("transformers"));
        Assertions.assertTrue(ex.getMessage().contains("computeBlocks"));
    }

    @Test
    void rejectsCyclicTransformGraphInComputeBlock() {
        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("dag-block");
        block.setSources(Map.of("seed", numberSource()));
        block.setTransformGraph(cyclicGraph());

        TemplateV2VO template = new TemplateV2VO();
        template.setName("wf-dag-cycle");
        template.setWorkflow(singlePauseWorkflow());
        template.setComputeBlocks(List.of(block));

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TemplateV2Validator.validate(template));

        Assertions.assertTrue(ex.getMessage().contains("computeBlocks[0].transformGraph"));
        Assertions.assertTrue(ex.getMessage().contains("cycle"));
    }

    @Test
    void rejectsJsTransformWithoutScriptBodyInComputeBlockGraph() {
        JsTransformVO jsTransform = new JsTransformVO();
        jsTransform.setName("row-js");

        TransformNodeVO node = new TransformNodeVO();
        node.setId("n1");
        node.setTransformId("row-js");

        TransformGraphVO graph = new TransformGraphVO();
        graph.setTransforms(Map.of("row-js", jsTransform));
        graph.setNodes(List.of(node));
        graph.setEdges(List.of());

        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("js-block");
        block.setSources(Map.of("seed", numberSource()));
        block.setTransformGraph(graph);

        TemplateV2VO template = new TemplateV2VO();
        template.setName("wf-js-blank");
        template.setWorkflow(singlePauseWorkflow());
        template.setComputeBlocks(List.of(block));

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TemplateV2Validator.validate(template));

        Assertions.assertTrue(ex.getMessage().contains("computeBlocks[0].transformGraph.transforms.row-js.script"));
        Assertions.assertTrue(ex.getMessage().contains("must not be blank"));
    }

    @Test
    void rejectsTopLevelJsTransformWithoutScriptBodyWithJsonPath() {
        JsTransformVO jsTransform = new JsTransformVO();

        TemplateV2VO template = new TemplateV2VO();
        template.setName("linear-js-blank");
        template.setSources(Map.of("input", numberSource()));
        template.setTransformers(List.of(jsTransform));
        template.setSinks(List.of(consoleSink()));

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TemplateV2Validator.validate(template));

        Assertions.assertTrue(ex.getMessage().contains("transformers[0].script"));
        Assertions.assertTrue(ex.getMessage().contains("must not be blank"));
    }

    @Test
    void rejectsEmptyWorkflowStepsWithJsonPath() {
        WorkflowSpecVO workflow = new WorkflowSpecVO();
        workflow.setSteps(List.of());

        TemplateV2VO template = new TemplateV2VO();
        template.setName("wf-empty-steps");
        template.setWorkflow(workflow);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TemplateV2Validator.validate(template));

        Assertions.assertTrue(ex.getMessage().contains("workflow.steps"));
    }

    private static TemplateV2VO workflowTemplateWithComputeBlock() {
        InvokeComputeBlockStepVO invoke = new InvokeComputeBlockStepVO();
        invoke.setId("invoke-1");
        invoke.setComputeBlockId("seed-block");

        WorkflowSpecVO workflow = new WorkflowSpecVO();
        workflow.setSteps(List.of(invoke));

        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("seed-block");
        block.setSources(Map.of("seed", numberSource()));
        block.setTransformers(List.of(sql("SELECT value FROM seed")));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("wf-valid");
        template.setWorkflow(workflow);
        template.setComputeBlocks(List.of(block));
        return template;
    }

    private static WorkflowSpecVO singlePauseWorkflow() {
        PauseStepVO pause = new PauseStepVO();
        pause.setId("pause-1");
        pause.setDurationMs(1L);

        WorkflowSpecVO workflow = new WorkflowSpecVO();
        workflow.setSteps(List.of(pause));
        return workflow;
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

    private static IteratorSourceVO numberSource() {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(1L);
        iterator.setTo(3L);
        iterator.setStep(1);
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
}
