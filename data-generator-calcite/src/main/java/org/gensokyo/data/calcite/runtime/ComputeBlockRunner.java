/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.kit.collect.CollectKit;

import java.util.List;
import java.util.Map;

/**
 * Executes a compute block: materialize sources, run an L1 transform DAG or linear transformers fallback, then sinks.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class ComputeBlockRunner {

    private final InMemoryPipeline.RowSinkFactory rowSinkFactory;
    private final TransformDagExecutor transformDagExecutor;

    /**
     * Creates a runner that writes sinks through the runtime registry.
     */
    public ComputeBlockRunner() {
        this((registry, writer) -> registry.createSink(writer));
    }

    /**
     * Creates a runner with a custom sink factory (typical in tests).
     *
     * @param rowSinkFactory sink factory
     */
    public ComputeBlockRunner(InMemoryPipeline.RowSinkFactory rowSinkFactory) {
        this(rowSinkFactory, new TransformDagExecutor());
    }

    /**
     * Creates a runner with explicit sink and DAG executor collaborators.
     *
     * @param rowSinkFactory      sink factory
     * @param transformDagExecutor DAG executor
     */
    public ComputeBlockRunner(InMemoryPipeline.RowSinkFactory rowSinkFactory, TransformDagExecutor transformDagExecutor) {
        this.rowSinkFactory = rowSinkFactory;
        this.transformDagExecutor = transformDagExecutor;
    }

    /**
     * Runs one compute block using the supplied execution policy and runtime registry.
     *
     * @param block    compute block definition
     * @param policy   resolved execution policy
     * @param registry runtime registry for sources, transforms, and sinks
     * @return transform output and run metrics
     * @throws IllegalArgumentException when the block has no transforms configured
     */
    public TemplateV2RunResult run(
            ComputeBlockVO block,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry) {
        if (block == null) {
            throw new IllegalArgumentException("Compute block must not be null");
        }
        if (CollectKit.isEmpty(block.getSources())) {
            throw new IllegalArgumentException("Compute block sources must not be empty");
        }
        if (policy.partitionCount() > 1) {
            return new PartitionedComputeBlockRunner(rowSinkFactory, transformDagExecutor)
                    .run(block, policy, registry);
        }

        RunMetrics metrics = new RunMetrics(policy.mode());
        CalciteExecutionContext context = materializeSources(block, policy, registry, metrics);
        CalciteRowTransformer.TransformResult result = applyTransforms(block, context, registry);
        writeSinks(block, registry, result, metrics);
        return new TemplateV2RunResult(result.schema(), result.rows(), metrics);
    }

    private CalciteExecutionContext materializeSources(
            ComputeBlockVO block,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry,
            RunMetrics metrics) {
        CalciteExecutionContext context = new CalciteExecutionContext();
        for (Map.Entry<String, SourceVO> entry : block.getSources().entrySet()) {
            String sourceName = entry.getKey();
            RowSource rowSource = registry.createSource(sourceName, entry.getValue(), policy);
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
        return context;
    }

    private CalciteRowTransformer.TransformResult applyTransforms(
            ComputeBlockVO block,
            CalciteExecutionContext context,
            TemplateV2RuntimeRegistry registry) {
        TransformGraphVO graph = block.getTransformGraph();
        if (graph != null && graph.getNodes() != null && !graph.getNodes().isEmpty()) {
            return transformDagExecutor.execute(graph, context, registry);
        }
        if (block.getTransformers() == null || block.getTransformers().isEmpty()) {
            throw new IllegalArgumentException("Compute block requires transformGraph nodes or linear transformers");
        }
        return applyLinearTransformers(block.getTransformers(), context, registry);
    }

    private CalciteRowTransformer.TransformResult applyLinearTransformers(
            List<TransformVO> transformers,
            CalciteExecutionContext context,
            TemplateV2RuntimeRegistry registry) {
        CalciteRowTransformer.TransformResult current = null;
        for (TransformVO transformer : transformers) {
            current = registry.applyTransform(transformer, context);
            context = new CalciteExecutionContext()
                    .addTable("input", current.schema(), current.rows())
                    .addTable("current", current.schema(), current.rows());
        }
        if (current == null) {
            throw new IllegalStateException("Compute block linear transformers produced no result");
        }
        return current;
    }

    private void writeSinks(
            ComputeBlockVO block,
            TemplateV2RuntimeRegistry registry,
            CalciteRowTransformer.TransformResult result,
            RunMetrics metrics) {
        if (block.getSinks() == null || block.getSinks().isEmpty()) {
            return;
        }
        TemplateV2VO sinkTemplate = new TemplateV2VO();
        sinkTemplate.setSinks(block.getSinks());
        SinkWriteExecutor.writeSinks(rowSinkFactory, registry, sinkTemplate, result, metrics, 0);
    }
}
