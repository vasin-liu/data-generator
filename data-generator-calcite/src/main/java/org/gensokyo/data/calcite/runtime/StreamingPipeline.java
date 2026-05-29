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
import org.gensokyo.data.calcite.sql.ExecutionShape;
import org.gensokyo.data.calcite.sql.ExecutionShapeClassifier;
import org.gensokyo.data.calcite.source.ChunkedRowSource;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Streaming Template V2 pipeline (v1): reads a single JDBC query source in chunks, applies row-local SQL
 * per chunk, and writes JDBC sinks in bounded batches while tracking peak in-memory row usage.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class StreamingPipeline {

    /**
     * Creates sinks through the runtime registry (default for standalone use).
     */
    public StreamingPipeline() {
        this((registry, writer) -> registry.createSink(writer));
    }

    /**
     * Creates sinks using the supplied factory (e.g. {@link TemplateV2Runner#createSink} for test overrides).
     *
     * @param rowSinkFactory sink factory
     */
    public StreamingPipeline(InMemoryPipeline.RowSinkFactory rowSinkFactory) {
        this.rowSinkFactory = rowSinkFactory;
    }

    private final InMemoryPipeline.RowSinkFactory rowSinkFactory;

    /**
     * Runs the template in streaming mode with the given effective execution policy.
     *
     * @param template template definition
     * @param policy   resolved execution policy (mode must be {@code STREAMING})
     * @param registry runtime registry for sources, transforms, and sinks
     * @return run result with empty rows and collected metrics
     * @throws IllegalArgumentException when v1 scope is exceeded (multi-source, broadcast join, or non-row-local SQL)
     */
    public TemplateV2RunResult run(
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry) {
        if (template.getTransformers().isEmpty()) {
            throw new IllegalArgumentException("Current V2 runner requires at least one transformer");
        }

        validateV1Scope(template);

        Map.Entry<String, QuerySourceVO> queryEntry = soleQuerySource(template);
        String sourceName = queryEntry.getKey();
        QuerySourceVO querySource = queryEntry.getValue();

        RunMetrics metrics = new RunMetrics(policy.mode());
        RowSource rowSource = registry.createSource(sourceName, querySource, policy);
        if (!(rowSource instanceof ChunkedRowSource chunked)) {
            throw new IllegalStateException(
                    "STREAMING mode requires a chunked row source for [" + sourceName + "], got "
                            + rowSource.getClass().getName());
        }

        TransformVO transformer = template.getTransformers().getFirst();
        RowSchema lastSchema = null;
        int chunkSize = policy.sourceChunkSize();
        int sinkBatchSize = policy.sinkBatchSize();

        while (chunked.hasNextChunk()) {
            var chunk = chunked.nextChunk(chunkSize);
            if (chunk.isEmpty()) {
                continue;
            }
            metrics.incrementChunks();
            metrics.addRead(sourceName, chunk.size());
            if (metrics.getTotalRowsRead() > policy.maxRowsInMemory() && policy.failOnLimitExceeded()) {
                throw new ScaleLimitExceededException(
                        "maxRowsInMemory",
                        policy.maxRowsInMemory(),
                        metrics.getTotalRowsRead(),
                        "SOURCE_READ",
                        sourceName);
            }

            RowSchema chunkSchema = chunked.schema() != null ? chunked.schema() : rowSource.schema();
            CalciteExecutionContext context = new CalciteExecutionContext()
                    .addTable(sourceName, chunkSchema, chunk);
            CalciteRowTransformer.TransformResult current =
                    registry.applyTransform(transformer, context);
            lastSchema = current.schema();

            // Peak tracks the largest transformed chunk held before sink flush completes.
            metrics.recordPeakRowsInMemory(current.rows().size());
            metrics.addRowsWritten(current.rows().size());
            writeSinks(registry, template, current, sinkBatchSize);
        }

        return new TemplateV2RunResult(lastSchema, List.of(), metrics);
    }

    private static void validateV1Scope(TemplateV2VO template) {
        int sourceCount = template.getSources() == null ? 0 : template.getSources().size();
        if (sourceCount != 1) {
            throw new IllegalArgumentException(
                    "STREAMING mode v1 requires exactly one source, found " + sourceCount);
        }

        ExecutionShape shape = ExecutionShapeClassifier.classify(template);
        if (shape == ExecutionShape.BROADCAST_JOIN) {
            throw new IllegalArgumentException(
                    "STREAMING mode v1 does not support BROADCAST_JOIN execution shape");
        }
        if (shape != ExecutionShape.ROW_LOCAL) {
            throw new IllegalArgumentException(
                    "STREAMING mode v1 requires ROW_LOCAL execution shape, got " + shape);
        }
    }

    private void writeSinks(
            TemplateV2RuntimeRegistry registry,
            TemplateV2VO template,
            CalciteRowTransformer.TransformResult result,
            int sinkBatchSize) {
        SinkPolicyMode mode = sinkPolicyMode(template.getSinkExecutionPolicy());
        int sinkIndex = 0;
        for (WriteStageVO sink : template.getSinks()) {
            int writerIndex = 0;
            for (WriterVO writer : sink.getWriters()) {
                try {
                    rowSinkFactory.create(registry, writer)
                            .writeBatch(result.schema(), result.rows(), sinkBatchSize);
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

    private static Map.Entry<String, QuerySourceVO> soleQuerySource(TemplateV2VO template) {
        Map.Entry<String, SourceVO> entry = template.getSources().entrySet().iterator().next();
        if (!(entry.getValue() instanceof QuerySourceVO querySource)) {
            throw new IllegalArgumentException(
                    "STREAMING mode v1 requires a QuerySourceVO source, got "
                            + entry.getValue().getClass().getSimpleName());
        }
        return Map.entry(entry.getKey(), querySource);
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
}
