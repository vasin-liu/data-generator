/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.task;

import org.gensokyo.data.calcite.runtime.RunMetrics;
import org.gensokyo.data.calcite.runtime.SinkWriteMetric;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.model.v2.StageMetricVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
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
            sources.add(new StageMetricVO(entry.getKey(), entry.getValue(), null, null));
        }

        List<StageMetricVO> transformers = buildTransformerMetrics(template, outputRows);
        List<StageMetricVO> sinks = buildSinkMetrics(template, metrics, outputRows);

        return new RunReportVO(
                List.copyOf(sources),
                List.copyOf(transformers),
                List.copyOf(sinks),
                metrics.getExecutionMode(),
                durationMs,
                List.copyOf(collectErrorSamples(metrics)));
    }

    private static List<StageMetricVO> buildTransformerMetrics(TemplateV2VO template, long outputRows) {
        List<StageMetricVO> transformers = new ArrayList<>();
        if (template.getTransformers() == null) {
            return transformers;
        }
        for (int index = 0; index < template.getTransformers().size(); index++) {
            TransformVO transformer = template.getTransformers().get(index);
            transformers.add(new StageMetricVO(transformName(transformer, index), outputRows, null, null));
        }
        return transformers;
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
                    sinkMetric.getLastErrorSample()));
        }
        if (!sinks.isEmpty()) {
            return sinks;
        }
        if (template.getSinks() == null) {
            return sinks;
        }
        long written = metrics.getRowsWritten() > 0 ? metrics.getRowsWritten() : outputRows;
        for (int index = 0; index < template.getSinks().size(); index++) {
            sinks.add(new StageMetricVO("sink[" + index + "]", written, null, null));
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
