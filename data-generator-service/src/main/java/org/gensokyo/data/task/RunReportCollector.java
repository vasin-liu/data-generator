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
import org.gensokyo.data.model.v2.TransformErrorVO;
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

    /**
     * Builds a minimal failure report carrying one structured transform error so the operator console
     * can surface an actionable failure (D-08) even when the run produced no metrics.
     *
     * @param template   executed template definition (may be {@code null} when the failure preceded load)
     * @param error      terminal run exception
     * @param durationMs elapsed wall-clock time before the failure
     * @return report with a populated {@code transformErrors} list, or {@code null} when {@code error} is null
     */
    public RunReportVO collectFailure(TemplateV2VO template, Throwable error, long durationMs) {
        if (error == null) {
            return null;
        }
        SinkFailureDetails sinkFailure = parseSinkFailure(error);
        if (sinkFailure != null) {
            String safeMessage = sinkFailure.actionableMessage().length() > 2000
                    ? sinkFailure.actionableMessage().substring(0, 2000)
                    : sinkFailure.actionableMessage();
            StageMetricVO sinkMetric = new StageMetricVO(
                    sinkFailure.sinkKey(),
                    0L,
                    null,
                    safeMessage,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L);
            return new RunReportVO(
                    List.of(),
                    List.of(),
                    List.of(sinkMetric),
                    null,
                    durationMs,
                    List.of(safeMessage),
                    List.of(),
                    List.of());
        }
        // The runtime registry wraps factory failures as "... for type [<type>] and model [<model>]";
        // walk the cause chain to recover the operator type and the underlying root-cause message.
        String operatorType = null;
        String rootMessage = null;
        for (Throwable t = error; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message == null) {
                continue;
            }
            int token = message.indexOf("for type [");
            if (token >= 0 && operatorType == null) {
                int start = token + "for type [".length();
                int end = message.indexOf(']', start);
                if (end > start) {
                    operatorType = message.substring(start, end);
                }
                // Prefer the wrapped cause's message as the actionable root cause.
                rootMessage = t.getCause() != null ? t.getCause().getMessage() : message;
            }
            rootMessage = rootMessage == null ? message : rootMessage;
        }
        if (rootMessage == null) {
            rootMessage = error.toString();
        }

        String step = "transform";
        String operatorName = null;
        if (operatorType != null && template != null && template.getTransformers() != null) {
            for (int index = 0; index < template.getTransformers().size(); index++) {
                TransformVO transformer = template.getTransformers().get(index);
                if (operatorType.equals(transformer.getType())) {
                    step = "transformers[" + index + "]";
                    if (transformer.getName() != null && !transformer.getName().isBlank()) {
                        operatorName = transformer.getName();
                    }
                    break;
                }
            }
        }

        String safeMessage = rootMessage.length() > 2000 ? rootMessage.substring(0, 2000) : rootMessage;
        TransformErrorVO transformError = new TransformErrorVO(
                step, operatorType, operatorName, safeMessage, null, null);
        return new RunReportVO(
                List.of(),
                List.of(),
                List.of(),
                null,
                durationMs,
                List.of(safeMessage),
                List.of(),
                List.of(transformError));
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
                    sinkMetric.getRowsFailed(),
                    sinkMetric.getRowsRead(),
                    sinkMetric.getRowsUpserted(),
                    sinkMetric.getRowsSkipped()));
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

    private static SinkFailureDetails parseSinkFailure(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null || !message.startsWith("Failed to execute Template V2 sink writer")) {
                continue;
            }
            Integer sinkIndex = parseBracketedIndex(message, "sink index [");
            Integer writerIndex = parseBracketedIndex(message, "writer index [");
            String type = parseBracketedToken(message, "type [");
            String target = parseBracketedToken(message, "target [");
            String sinkKey = sinkIndex != null && writerIndex != null
                    ? "sink[" + sinkIndex + "].writer[" + writerIndex + "]"
                    : "sink";
            String root = current.getCause() != null && current.getCause().getMessage() != null
                    ? current.getCause().getMessage()
                    : message;
            String actionable = sinkKey
                    + (type != null ? " (type=" + type : "")
                    + (target != null ? (type != null ? ", target=" : " (target=") + target : "")
                    + (type != null || target != null ? ")" : "")
                    + ": " + root;
            return new SinkFailureDetails(sinkKey, actionable);
        }
        return null;
    }

    private static Integer parseBracketedIndex(String message, String prefix) {
        int start = message.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = message.indexOf(']', start);
        if (end <= start) {
            return null;
        }
        try {
            return Integer.parseInt(message.substring(start, end).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String parseBracketedToken(String message, String prefix) {
        int start = message.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = message.indexOf(']', start);
        if (end <= start) {
            return null;
        }
        return message.substring(start, end).trim();
    }

    private record SinkFailureDetails(String sinkKey, String actionableMessage) {
    }
}
