/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.RowSink;
import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.join.BroadcastJoinExecutor;
import org.gensokyo.data.calcite.join.BroadcastJoinSnapshot;
import org.gensokyo.data.calcite.join.BroadcastJoinSpec;
import org.gensokyo.data.calcite.join.BroadcastJoinSqlParser;
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
 * Chunked Template V2 pipeline for {@link ExecutionShape#ROW_LOCAL} and {@link ExecutionShape#BROADCAST_JOIN}:
 * reads JDBC query sources in chunks, applies SQL per chunk (or broadcast-joins fact chunks to a materialized
 * dimension), and writes sinks in batches without retaining all rows.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class ChunkedPipeline {

    /**
     * Creates sinks through the runtime registry (default for standalone use).
     */
    public ChunkedPipeline() {
        this((registry, writer) -> registry.createSink(writer));
    }

    /**
     * Creates sinks using the supplied factory (e.g. {@link TemplateV2Runner#createSink} for test overrides).
     *
     * @param rowSinkFactory sink factory
     */
    public ChunkedPipeline(InMemoryPipeline.RowSinkFactory rowSinkFactory) {
        this.rowSinkFactory = rowSinkFactory;
    }

    private final InMemoryPipeline.RowSinkFactory rowSinkFactory;

    /**
     * Runs the template in chunked mode with the given effective execution policy.
     *
     * @param template template definition
     * @param policy   resolved execution policy (mode must be {@code CHUNKED})
     * @param registry runtime registry for sources, transforms, and sinks
     * @return run result with empty rows and collected metrics
     */
    public TemplateV2RunResult run(
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry) {
        if (template.getTransformers().isEmpty()) {
            throw new IllegalArgumentException("Current V2 runner requires at least one transformer");
        }

        ExecutionShape shape = ExecutionShapeClassifier.classify(template);
        return switch (shape) {
            case ROW_LOCAL -> runRowLocal(template, policy, registry);
            case BROADCAST_JOIN -> runBroadcastJoin(template, policy, registry);
            default -> throw new IllegalStateException(
                    "CHUNKED mode requires ROW_LOCAL or BROADCAST_JOIN execution shape, got " + shape);
        };
    }

    private TemplateV2RunResult runRowLocal(
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry) {
        Map.Entry<String, QuerySourceVO> queryEntry = soleQuerySource(template);
        String sourceName = queryEntry.getKey();
        QuerySourceVO querySource = queryEntry.getValue();

        RunMetrics metrics = new RunMetrics(policy.mode());
        var rowSource = registry.createSource(sourceName, querySource, policy);
        if (!(rowSource instanceof ChunkedRowSource chunked)) {
            throw new IllegalStateException(
                    "CHUNKED mode requires a chunked row source for [" + sourceName + "], got "
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
            ExecutionGuard.checkMaxTotalRows(template, policy, metrics);

            RowSchema chunkSchema = chunked.schema() != null ? chunked.schema() : rowSource.schema();
            CalciteExecutionContext context = new CalciteExecutionContext()
                    .addTable(sourceName, chunkSchema, chunk);
            CalciteRowTransformer.TransformResult current =
                    registry.applyTransform(transformer, context);
            lastSchema = current.schema();
            writeSinks(registry, template, current, sinkBatchSize);
        }

        return new TemplateV2RunResult(lastSchema, List.of(), metrics);
    }

    private TemplateV2RunResult runBroadcastJoin(
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry) {
        BroadcastJoinSpec spec = BroadcastJoinSqlParser.parse(template);
        Map<String, SourceVO> sources = template.getSources();
        QuerySourceVO dimSourceVo = (QuerySourceVO) sources.get(spec.dimSourceName());
        QuerySourceVO factSourceVo = (QuerySourceVO) sources.get(spec.factSourceName());

        RunMetrics metrics = new RunMetrics(policy.mode());
        int sinkBatchSize = policy.sinkBatchSize();
        int chunkSize = policy.sourceChunkSize();

        // Materialize dimension in memory (non-chunked read).
        RowSource dimRowSource = registry.createSource(spec.dimSourceName(), dimSourceVo, null);
        BroadcastJoinSnapshot snapshot = BroadcastJoinSnapshot.materialize(
                dimRowSource,
                spec.dimJoinColumn(),
                policy.broadcastMaxRows(),
                spec.dimSourceName());
        metrics.addRead(spec.dimSourceName(), dimRowSource.rows().size());
        ExecutionGuard.checkMaxTotalRows(template, policy, metrics);

        RowSource factRowSource = registry.createSource(spec.factSourceName(), factSourceVo, policy);
        if (!(factRowSource instanceof ChunkedRowSource chunked)) {
            throw new IllegalStateException(
                    "CHUNKED broadcast join requires a chunked fact source for ["
                            + spec.factSourceName() + "], got " + factRowSource.getClass().getName());
        }

        RowSchema lastSchema = spec.outputSchema();
        while (chunked.hasNextChunk()) {
            var chunk = chunked.nextChunk(chunkSize);
            if (chunk.isEmpty()) {
                continue;
            }
            metrics.incrementChunks();
            metrics.addRead(spec.factSourceName(), chunk.size());
            if (metrics.getTotalRowsRead() > policy.maxRowsInMemory() && policy.failOnLimitExceeded()) {
                throw new ScaleLimitExceededException(
                        "maxRowsInMemory",
                        policy.maxRowsInMemory(),
                        metrics.getTotalRowsRead(),
                        "SOURCE_READ",
                        spec.factSourceName());
            }
            ExecutionGuard.checkMaxTotalRows(template, policy, metrics);

            CalciteRowTransformer.TransformResult joined =
                    BroadcastJoinExecutor.join(chunk, snapshot, spec);
            lastSchema = joined.schema();
            writeSinks(registry, template, joined, sinkBatchSize);
        }

        return new TemplateV2RunResult(lastSchema, List.of(), metrics);
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
        if (template.getSources() == null || template.getSources().size() != 1) {
            int count = template.getSources() == null ? 0 : template.getSources().size();
            throw new IllegalStateException(
                    "CHUNKED ROW_LOCAL mode requires exactly one source entry, found " + count);
        }
        Map.Entry<String, SourceVO> entry = template.getSources().entrySet().iterator().next();
        if (!(entry.getValue() instanceof QuerySourceVO querySource)) {
            throw new IllegalStateException(
                    "CHUNKED mode requires a QuerySourceVO source, got "
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
