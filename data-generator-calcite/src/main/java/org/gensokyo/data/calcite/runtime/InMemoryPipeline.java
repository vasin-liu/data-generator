/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.RowSink;
import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.Locale;
import java.util.Map;

/**
 * In-memory Template V2 pipeline: loads all sources into context, applies transforms, writes sinks.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class InMemoryPipeline {

    /**
     * Creates sinks through the runtime registry (default for standalone use).
     */
    public InMemoryPipeline() {
        this((registry, writer) -> registry.createSink(writer));
    }

    /**
     * Creates sinks using the supplied factory (e.g. {@link TemplateV2Runner#createSink} for test overrides).
     *
     * @param rowSinkFactory sink factory
     */
    public InMemoryPipeline(RowSinkFactory rowSinkFactory) {
        this.rowSinkFactory = rowSinkFactory;
    }

    private final RowSinkFactory rowSinkFactory;

    /**
     * Runs the template in memory with the given effective execution policy.
     *
     * @param template template definition
     * @param policy resolved execution policy
     * @param registry runtime registry for sources, transforms, and sinks
     * @return transform result and run metrics
     */
    public TemplateV2RunResult run(
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry) {
        if (template.getTransformers().isEmpty()) {
            throw new IllegalArgumentException("Current V2 runner requires at least one transformer");
        }

        RunMetrics metrics = new RunMetrics(policy.mode());
        CalciteExecutionContext context = new CalciteExecutionContext();
        for (Map.Entry<String, SourceVO> entry : template.getSources().entrySet()) {
            String sourceName = entry.getKey();
            RowSource rowSource = registry.createSource(sourceName, entry.getValue());
            int count = rowSource.rows().size();
            metrics.addRead(sourceName, count);
            if (metrics.getTotalRowsRead() > policy.maxRowsInMemory() && policy.failOnLimitExceeded()) {
                throw new ScaleLimitExceededException(
                        "maxRowsInMemory",
                        policy.maxRowsInMemory(),
                        metrics.getTotalRowsRead(),
                        "SOURCE_READ",
                        sourceName);
            }
            context.addSource(rowSource);
        }

        CalciteRowTransformer.TransformResult current = null;
        for (TransformVO transformer : template.getTransformers()) {
            current = registry.applyTransform(transformer, context);
            context = new CalciteExecutionContext()
                    .addTable("input", current.schema(), current.rows())
                    .addTable("current", current.schema(), current.rows());
        }

        if (current == null) {
            throw new IllegalStateException("Current V2 runner produced no transform result");
        }

        writeSinks(registry, template, current);
        return new TemplateV2RunResult(current.schema(), current.rows(), metrics);
    }

    private void writeSinks(
            TemplateV2RuntimeRegistry registry,
            TemplateV2VO template,
            CalciteRowTransformer.TransformResult result) {
        SinkPolicyMode mode = sinkPolicyMode(template.getSinkExecutionPolicy());
        int sinkIndex = 0;
        for (WriteStageVO sink : template.getSinks()) {
            int writerIndex = 0;
            for (WriterVO writer : sink.getWriters()) {
                try {
                    rowSinkFactory.create(registry, writer).write(result.schema(), result.rows());
                } catch (RuntimeException e) {
                    if (mode == SinkPolicyMode.CONTINUE_ON_ERROR) {
                        writerIndex++;
                        continue;
                    }
                    throw sinkWriteFailure(sinkIndex, writerIndex, writer, e);
                }
                writerIndex++;
            }
            sinkIndex++;
        }
    }

    private static SinkPolicyMode sinkPolicyMode(SinkExecutionPolicyVO policy) {
        if (policy == null || policy.getMode() == null || policy.getMode().isBlank()) {
            return SinkPolicyMode.FAIL_FAST;
        }
        return SinkPolicyMode.valueOf(policy.getMode().trim().toUpperCase(Locale.ROOT));
    }

    private static IllegalStateException sinkWriteFailure(
            int sinkIndex,
            int writerIndex,
            WriterVO writer,
            RuntimeException cause) {
        return new IllegalStateException("Failed to execute Template V2 sink writer"
                + " at sink index [" + sinkIndex + "]"
                + ", writer index [" + writerIndex + "]"
                + ", type [" + writer.getType() + "]"
                + ", model [" + writer.getClass().getName() + "]"
                + ", target [" + writer.getTarget() + "]", cause);
    }

    private enum SinkPolicyMode {
        FAIL_FAST,
        CONTINUE_ON_ERROR
    }

    /**
     * Factory for row sinks, allowing {@link TemplateV2Runner} to delegate {@code createSink} overrides.
     */
    @FunctionalInterface
    public interface RowSinkFactory {
        /**
         * Creates a sink for the given writer.
         *
         * @param registry runtime registry
         * @param writer writer configuration
         * @return row sink
         */
        RowSink create(TemplateV2RuntimeRegistry registry, WriterVO writer);
    }
}
