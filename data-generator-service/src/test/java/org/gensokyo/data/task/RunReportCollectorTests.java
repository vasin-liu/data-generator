/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.ai.usage.AiPricingService;
import org.gensokyo.data.calcite.runtime.AiCallMetric;
import org.gensokyo.data.calcite.runtime.RunMetrics;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.model.v2.StageMetricVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformNodeVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RunReportCollector} transformer metric aggregation.
 *
 * @author Gensokyo
 * @since 2026-06-02
 */
class RunReportCollectorTests {

    private final RunReportCollector collector = new RunReportCollector(new AiPricingService(new DataGeneratorProperties()));

    /**
     * Linear template transformers remain in the report alongside compute-block DAG nodes.
     */
    @Test
    void includesComputeBlockDagNodesInTransformerMetrics() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("dag-report");

        SqlTransformVO linear = new SqlTransformVO();
        linear.setType("sql");
        linear.setName("top-level");
        template.setTransformers(List.of(linear));

        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("dag-block");

        TransformGraphVO graph = new TransformGraphVO();
        Map<String, SqlTransformVO> transforms = new LinkedHashMap<>();
        SqlTransformVO filter = new SqlTransformVO();
        filter.setType("sql");
        filter.setName("filter-high");
        transforms.put("filter-high", filter);
        SqlTransformVO shift = new SqlTransformVO();
        shift.setType("sql");
        shift.setName("shift-values");
        transforms.put("shift-values", shift);
        graph.setTransforms(new LinkedHashMap<>(transforms));

        TransformNodeVO n1 = new TransformNodeVO();
        n1.setId("n1");
        n1.setTransformId("filter-high");
        TransformNodeVO n2 = new TransformNodeVO();
        n2.setId("n2");
        n2.setTransformId("shift-values");
        graph.setNodes(List.of(n1, n2));
        block.setTransformGraph(graph);
        template.setComputeBlocks(List.of(block));

        RunMetrics metrics = new RunMetrics("IN_MEMORY");
        metrics.addRead("seed", 5);
        metrics.addRowsWritten(2);
        TemplateV2RunResult result = new TemplateV2RunResult(null, List.of(), metrics);

        RunReportVO report = collector.collect(template, result, 42L);
        assertThat(report).isNotNull();

        List<String> names = report.transformers().stream().map(StageMetricVO::name).toList();
        assertThat(names).containsExactly(
                "top-level",
                "dag-block/n1 (filter-high)",
                "dag-block/n2 (shift-values)");
        assertThat(report.transformers().get(1).rowsProcessed()).isEqualTo(2L);
    }

    /**
     * CONTINUE_ON_ERROR sink metrics expose per-writer ok/failed row counts in the run report.
     */
    @Test
    void exposesPartialSuccessSinkMetrics() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("partial-sink-report");

        RunMetrics metrics = new RunMetrics("IN_MEMORY");
        metrics.recordSinkRowsOk("sink[0].writer[0]", 3L);
        metrics.recordSinkRowsFailed("sink[0].writer[0]", 2L, "duplicate key");
        metrics.recordSinkRowsOk("sink[1].writer[0]", 5L);
        TemplateV2RunResult result = new TemplateV2RunResult(null, List.of(), metrics);

        RunReportVO report = collector.collect(template, result, 10L);
        assertThat(report).isNotNull();
        assertThat(report.sinks()).hasSize(2);

        StageMetricVO failingWriter = report.sinks().get(0);
        assertThat(failingWriter.name()).isEqualTo("sink[0].writer[0]");
        assertThat(failingWriter.rowsProcessed()).isEqualTo(5L);
        assertThat(failingWriter.rowsOk()).isEqualTo(3L);
        assertThat(failingWriter.rowsFailed()).isEqualTo(2L);
        assertThat(failingWriter.errorSample()).isEqualTo("duplicate key");

        StageMetricVO okWriter = report.sinks().get(1);
        assertThat(okWriter.rowsOk()).isEqualTo(5L);
        assertThat(okWriter.rowsFailed()).isEqualTo(0L);
    }

    /**
     * Extended sink counters (rowsRead, rowsUpserted, rowsSkipped) map into the run report (RW-04, D-16).
     */
    @Test
    void exposesExtendedSinkMetrics() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("upsert-report");

        RunMetrics metrics = new RunMetrics("CHUNKED");
        metrics.recordSinkRowsRead("sink[0].writer[0]", 10L);
        metrics.recordSinkRowsOk("sink[0].writer[0]", 9L);
        metrics.recordSinkRowsUpserted("sink[0].writer[0]", 3L);
        metrics.recordSinkRowsSkipped("sink[0].writer[0]", 1L);
        TemplateV2RunResult result = new TemplateV2RunResult(null, List.of(), metrics);

        RunReportVO report = collector.collect(template, result, 20L);
        assertThat(report).isNotNull();
        assertThat(report.sinks()).hasSize(1);

        StageMetricVO sink = report.sinks().getFirst();
        assertThat(sink.name()).isEqualTo("sink[0].writer[0]");
        assertThat(sink.rowsRead()).isEqualTo(10L);
        assertThat(sink.rowsUpserted()).isEqualTo(3L);
        assertThat(sink.rowsSkipped()).isEqualTo(1L);
        assertThat(sink.rowsOk()).isEqualTo(9L);
    }

    /**
     * Remote AI call diagnostics are surfaced in the structured run report.
     */
    @Test
    void exposesAiCallMetrics() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("ai-report");

        RunMetrics metrics = new RunMetrics("IN_MEMORY");
        metrics.recordAiCall(
                "ai_seed",
                AiCallMetric.remote("OLLAMA", "qwen2", 12L, 8L, 42L, 2, "alpha,beta"));
        TemplateV2RunResult result = new TemplateV2RunResult(null, List.of(), metrics);

        RunReportVO report = collector.collect(template, result, 15L);
        assertThat(report).isNotNull();
        assertThat(report.aiCalls()).hasSize(1);
        assertThat(report.aiCalls().getFirst().sourceName()).isEqualTo("ai_seed");
        assertThat(report.aiCalls().getFirst().providerType()).isEqualTo("OLLAMA");
        assertThat(report.aiCalls().getFirst().model()).isEqualTo("qwen2");
        assertThat(report.aiCalls().getFirst().promptTokens()).isEqualTo(12L);
        assertThat(report.aiCalls().getFirst().completionTokens()).isEqualTo(8L);
        assertThat(report.aiCalls().getFirst().latencyMs()).isEqualTo(42L);
        assertThat(report.aiCalls().getFirst().attempts()).isEqualTo(2);
        assertThat(report.aiCalls().getFirst().estimatedCostUsd()).isEqualTo(0.0D);
    }
}
