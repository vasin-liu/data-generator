/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import java.io.Serializable;
import java.util.List;

/**
 * Structured run report for Template V2 executions exposed to the operator console.
 *
 * @param sources        per-source read metrics
 * @param transformers   per-transform output metrics
 * @param sinks          per-sink write metrics
 * @param executionMode  resolved execution mode name
 * @param durationMs     total run duration in milliseconds
 * @param errorSamples    non-fatal warnings and sink failure samples
 * @param aiCalls         remote AI provider call diagnostics when collected
 * @param transformErrors structured transform/UDF failures for actionable surfacing (D-08)
 * @author Gensokyo
 * @since 2026-05-29
 */
public record RunReportVO(
        List<StageMetricVO> sources,
        List<StageMetricVO> transformers,
        List<StageMetricVO> sinks,
        String executionMode,
        Long durationMs,
        List<String> errorSamples,
        List<AiCallMetricVO> aiCalls,
        List<TransformErrorVO> transformErrors) implements Serializable {

    /**
     * Normalizes nullable collections for backward-compatible report deserialization.
     */
    public RunReportVO {
        if (aiCalls == null) {
            aiCalls = List.of();
        }
        if (transformErrors == null) {
            transformErrors = List.of();
        }
    }

    /**
     * Back-compatible constructor for callers that predate the {@code transformErrors} component.
     *
     * @param sources       per-source read metrics
     * @param transformers  per-transform output metrics
     * @param sinks         per-sink write metrics
     * @param executionMode resolved execution mode name
     * @param durationMs    total run duration in milliseconds
     * @param errorSamples  non-fatal warnings and sink failure samples
     * @param aiCalls       remote AI provider call diagnostics when collected
     */
    public RunReportVO(
            List<StageMetricVO> sources,
            List<StageMetricVO> transformers,
            List<StageMetricVO> sinks,
            String executionMode,
            Long durationMs,
            List<String> errorSamples,
            List<AiCallMetricVO> aiCalls) {
        this(sources, transformers, sinks, executionMode, durationMs, errorSamples, aiCalls, List.of());
    }
}
