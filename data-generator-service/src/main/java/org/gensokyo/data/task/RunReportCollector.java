/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.ai.usage.AiPricingService;
import org.gensokyo.data.calcite.runtime.AiCallMetric;
import org.gensokyo.data.calcite.runtime.RunMetrics;
import org.gensokyo.data.calcite.runtime.SinkWriteMetric;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.model.v2.AiCallMetricVO;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.model.v2.StageMetricVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformNodeVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds structured run reports from Template V2 runner results.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Component
public class RunReportCollector {

    private final AiPricingService aiPricingService;

    /**
     * @param aiPricingService token pricing for AI call cost estimates
     */
    public RunReportCollector(AiPricingService aiPricingService) {
        this.aiPricingService = aiPricingService;
    }

    /**
     * Aggregates runner metrics into a structured report for persistence and API exposure.
     *
     * @param template   executed template definition
     * @param result     runner output including metrics
     * @param durationMs elapsed wall-clock time for the run
     * @return structured report, or {@code null} when metrics are unavailable
     */
    public RunReportVO collect(TemplateV2VO template, TemplateV2RunResult result, long durationMs) {
        if (result == null || result.getMetrics() == null) {
            return null;
        }
        RunMetrics metrics = result.getMetrics();
        long outputRows = resolveOutputRows(result, metrics);

        List<StageMetricVO> sources = new ArrayList<>();
        for (Map.Entry<String, Long> entry : metrics.getRowsReadPerSource().entrySet()) {
            sources.add(new StageMetricVO(entry.getKey(), entry.getValue(), null, null, null, null));
        }

        List<StageMetricVO> transformers = buildTransformerMetrics(template, outputRows);
        List<StageMetricVO> sinks = buildSinkMetrics(template, metrics, outputRows);

        return new RunReportVO(
                List.copyOf(sources),
                List.copyOf(transformers),
                List.copyOf(sinks),
                metrics.getExecutionMode(),
                durationMs,
                List.copyOf(collectErrorSamples(metrics)),
                List.copyOf(buildAiCallMetrics(metrics)));
    }

    private List<AiCallMetricVO> buildAiCallMetrics(RunMetrics metrics) {
        List<AiCallMetricVO> aiCalls = new ArrayList<>();
        for (AiCallMetric metric : metrics.getAiCallMetrics().values()) {
            long promptTokens = metric.getPromptTokens();
            long completionTokens = metric.getCompletionTokens();
            double estimatedCostUsd = aiPricingService.estimateUsd(
                    metric.getProviderType(),
                    metric.getModel(),
                    promptTokens,
                    completionTokens);
            aiCalls.add(new AiCallMetricVO(
                    metric.getSourceName(),
                    metric.getProviderType(),
                    metric.getModel(),
                    metric.getPromptTokens(),
                    metric.getCompletionTokens(),
                    metric.getLatencyMs(),
                    metric.getAttempts(),
                    metric.getResponseSample(),
                    estimatedCostUsd));
        }
        return aiCalls;
    }

    private static List<StageMetricVO> buildTransformerMetrics(TemplateV2VO template, long outputRows) {
        List<StageMetricVO> transformers = new ArrayList<>();
        if (template.getTransformers() != null) {
            for (int index = 0; index < template.getTransformers().size(); index++) {
                TransformVO transformer = template.getTransformers().get(index);
                transformers.add(new StageMetricVO(transformName(transformer, index), outputRows, null, null, null, null));
            }
        }
        appendComputeBlockTransformMetrics(template, outputRows, transformers);
        return transformers;
    }

    private static void appendComputeBlockTransformMetrics(
            TemplateV2VO template,
            long outputRows,
            List<StageMetricVO> transformers) {
        if (template.getComputeBlocks() == null) {
            return;
        }
        for (ComputeBlockVO block : template.getComputeBlocks()) {
            TransformGraphVO graph = block.getTransformGraph();
            if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
                continue;
            }
            String blockPrefix = block.getId() != null && !block.getId().isBlank()
                    ? block.getId()
                    : "computeBlock";
            for (TransformNodeVO node : graph.getNodes()) {
                transformers.add(new StageMetricVO(
                        dagNodeMetricName(blockPrefix, node, graph),
                        outputRows,
                        null,
                        null,
                        null,
                        null));
            }
        }
    }

    private static String dagNodeMetricName(
            String blockPrefix,
            TransformNodeVO node,
            TransformGraphVO graph) {
        String nodeId = node.getId() != null && !node.getId().isBlank() ? node.getId() : "node";
        TransformVO transform = graph.getTransforms() != null && node.getTransformId() != null
                ? graph.getTransforms().get(node.getTransformId())
                : null;
        if (transform != null && transform.getName() != null && !transform.getName().isBlank()) {
            return blockPrefix + "/" + nodeId + " (" + transform.getName() + ")";
        }
        if (node.getTransformId() != null && !node.getTransformId().isBlank()) {
            return blockPrefix + "/" + nodeId + " [" + node.getTransformId() + "]";
        }
        return blockPrefix + "/" + nodeId;
    }

    private static List<StageMetricVO> buildSinkMetrics(TemplateV2VO template, RunMetrics metrics, long outputRows) {
        List<StageMetricVO> sinks = new ArrayList<>();
        for (Map.Entry<String, SinkWriteMetric> entry : metrics.getSinkMetrics().entrySet()) {
            SinkWriteMetric sinkMetric = entry.getValue();
            long processed = sinkMetric.getRowsOk() + sinkMetric.getRowsFailed();
            sinks.add(new StageMetricVO(
                    entry.getKey(),
                    processed,
                    null,
                    sinkMetric.getLastErrorSample(),
                    sinkMetric.getRowsOk(),
                    sinkMetric.getRowsFailed()));
        }
        if (!sinks.isEmpty()) {
            return sinks;
        }
        if (template.getSinks() == null) {
            return sinks;
        }
        long written = metrics.getRowsWritten() > 0 ? metrics.getRowsWritten() : outputRows;
        for (int index = 0; index < template.getSinks().size(); index++) {
            sinks.add(new StageMetricVO("sink[" + index + "]", written, null, null, null, null));
        }
        return sinks;
    }

    private static long resolveOutputRows(TemplateV2RunResult result, RunMetrics metrics) {
        if (metrics.getRowsWritten() > 0) {
            return metrics.getRowsWritten();
        }
        if (result.getRows() != null && !result.getRows().isEmpty()) {
            return result.getRows().size();
        }
        return metrics.getTotalRowsRead();
    }

    private static String transformName(TransformVO transformer, int index) {
        if (transformer.getName() != null && !transformer.getName().isBlank()) {
            return transformer.getName();
        }
        if (transformer.getType() != null && !transformer.getType().isBlank()) {
            return transformer.getType() + "[" + index + "]";
        }
        return "transform[" + index + "]";
    }

    private static List<String> collectErrorSamples(RunMetrics metrics) {
        List<String> samples = new ArrayList<>();
        for (SinkWriteMetric sinkMetric : metrics.getSinkMetrics().values()) {
            if (sinkMetric.getLastErrorSample() != null) {
                samples.add(sinkMetric.getLastErrorSample());
            }
        }
        samples.addAll(metrics.getWarnings());
        return samples;
    }
}
