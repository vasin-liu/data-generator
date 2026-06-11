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
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.workflow.BranchStepVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.model.v2.workflow.InvokeComputeBlockStepVO;
import org.gensokyo.data.model.v2.workflow.LogStepVO;
import org.gensokyo.data.model.v2.workflow.PauseStepVO;
import org.gensokyo.data.model.v2.workflow.SharedScopeStepVO;
import org.gensokyo.data.model.v2.workflow.WorkflowSpecVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link WorkflowRunner} L2 step execution ordering and semantics.
 */
class WorkflowRunnerTests {

    @Test
    void executesLogPauseInvokeComputeBlockLogInOrder() {
        List<String> collector = new ArrayList<>();
        long startNanos = System.nanoTime();

        TemplateV2RunResult result = new WorkflowRunner().run(
                workflowTemplate(),
                EffectiveExecutionPolicy.resolve(new ExecutionPolicyVO()),
                defaultRegistry(),
                collector::add);

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        Assertions.assertTrue(elapsedMs >= 45, "pause step should sleep at least ~50ms, was " + elapsedMs);

        Assertions.assertEquals(2, collector.size());
        Assertions.assertTrue(collector.get(0).contains("workflow-start"));
        Assertions.assertTrue(collector.get(1).contains("workflow-end"));

        Assertions.assertEquals(2, result.getMetrics().getWarnings().size());
        Assertions.assertTrue(result.getMetrics().getWarnings().get(0).contains("workflow-start"));
        Assertions.assertTrue(result.getMetrics().getWarnings().get(1).contains("workflow-end"));

        Assertions.assertEquals(3, result.getRows().size());
        Assertions.assertEquals("1", result.getRows().get(0).getString("value"));
    }

    @Test
    void branchStepRunsThenStepsWhenConditionTrue() {
        LogStepVO thenLog = logStep("then-log", "then-branch");
        LogStepVO elseLog = logStep("else-log", "else-branch");

        BranchStepVO branch = new BranchStepVO();
        branch.setId("branch-1");
        branch.setCondition("true");
        branch.setThenSteps(List.of(thenLog));
        branch.setElseSteps(List.of(elseLog));

        WorkflowSpecVO workflow = new WorkflowSpecVO();
        workflow.setSteps(List.of(branch));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("branch-template");
        template.setWorkflow(workflow);

        List<String> collector = new ArrayList<>();
        new WorkflowRunner().run(
                template,
                EffectiveExecutionPolicy.resolve(new ExecutionPolicyVO()),
                defaultRegistry(),
                collector::add);

        Assertions.assertEquals(1, collector.size());
        Assertions.assertTrue(collector.get(0).contains("then-branch"));
    }

    @Test
    void sharedScopeStepOpensWritesAndClosesScope() {
        SharedScopeStepVO open = new SharedScopeStepVO();
        open.setId("open-scope");
        open.setScopeId("run-scope");
        open.setAction("open");

        SharedScopeStepVO write = new SharedScopeStepVO();
        write.setId("write-scope");
        write.setScopeId("run-scope");
        write.setAction("write");
        write.setEntries(Map.of("flag", true));

        BranchStepVO branch = new BranchStepVO();
        branch.setId("read-scope");
        branch.setCondition("#shared['run-scope']['flag'] == true");
        branch.setThenSteps(List.of(logStep("scope-log", "scope-visible")));

        SharedScopeStepVO close = new SharedScopeStepVO();
        close.setId("close-scope");
        close.setScopeId("run-scope");
        close.setAction("close");

        WorkflowSpecVO workflow = new WorkflowSpecVO();
        workflow.setSteps(List.of(open, write, branch, close));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("shared-scope-template");
        template.setWorkflow(workflow);

        List<String> collector = new ArrayList<>();
        new WorkflowRunner().run(
                template,
                EffectiveExecutionPolicy.resolve(new ExecutionPolicyVO()),
                defaultRegistry(),
                collector::add);

        Assertions.assertEquals(1, collector.size());
        Assertions.assertTrue(collector.get(0).contains("scope-visible"));
    }

    @Test
    void templateV2RunnerDelegatesToWorkflowRunnerWhenWorkflowPresent() {
        TemplateV2RunResult result = new TemplateV2Runner(defaultRegistry()).run(workflowTemplate());
        Assertions.assertEquals(3, result.getRows().size());
        Assertions.assertEquals(2, result.getMetrics().getWarnings().size());
    }

    private static TemplateV2VO workflowTemplate() {
        LogStepVO start = logStep("log-start", "workflow-start");
        PauseStepVO pause = new PauseStepVO();
        pause.setId("pause-1");
        pause.setDurationMs(50L);

        InvokeComputeBlockStepVO invoke = new InvokeComputeBlockStepVO();
        invoke.setId("invoke-1");
        invoke.setComputeBlockId("seed-block");

        LogStepVO end = logStep("log-end", "workflow-end");

        WorkflowSpecVO workflow = new WorkflowSpecVO();
        workflow.setSteps(List.of(start, pause, invoke, end));

        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("seed-block");
        block.setSources(Map.of("seed", numberSource(1, 3, 1)));
        block.setTransformers(List.of(sql("SELECT value FROM seed")));

        TemplateV2VO template = new TemplateV2VO();
        template.setName("workflow-ordering");
        template.setWorkflow(workflow);
        template.setComputeBlocks(List.of(block));
        return template;
    }

    private static LogStepVO logStep(String id, String message) {
        LogStepVO log = new LogStepVO();
        log.setId(id);
        log.setLevel("INFO");
        log.setMessage(message);
        return log;
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

    private static TemplateV2RuntimeRegistry defaultRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}
