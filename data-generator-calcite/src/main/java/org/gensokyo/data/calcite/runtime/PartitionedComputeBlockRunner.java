/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.kit.collect.CollectKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes a compute block by splitting the primary source into partitions and processing them in parallel.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
public final class PartitionedComputeBlockRunner {

    private final InMemoryPipeline.RowSinkFactory rowSinkFactory;
    private final TransformDagExecutor transformDagExecutor;

    /**
     * Creates a runner that writes sinks through the runtime registry.
     */
    public PartitionedComputeBlockRunner() {
        this((registry, writer) -> registry.createSink(writer));
    }

    /**
     * Creates a runner with a custom sink factory (typical in tests).
     *
     * @param rowSinkFactory sink factory
     */
    public PartitionedComputeBlockRunner(InMemoryPipeline.RowSinkFactory rowSinkFactory) {
        this(rowSinkFactory, new TransformDagExecutor());
    }

    /**
     * Creates a runner with explicit sink and DAG executor collaborators.
     *
     * @param rowSinkFactory       sink factory
     * @param transformDagExecutor DAG executor
     */
    public PartitionedComputeBlockRunner(
            InMemoryPipeline.RowSinkFactory rowSinkFactory,
            TransformDagExecutor transformDagExecutor) {
        this.rowSinkFactory = rowSinkFactory;
        this.transformDagExecutor = transformDagExecutor;
    }

    /**
     * Runs one compute block using parallel in-process partitions.
     *
     * @param block    compute block definition
     * @param policy   resolved execution policy with {@code partitionCount > 1}
     * @param registry runtime registry for sources, transforms, and sinks
     * @return merged transform output and aggregated metrics
     */
    public TemplateV2RunResult run(
            ComputeBlockVO block,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry) {
        if (CollectKit.isEmpty(block.getSources())) {
            throw new IllegalArgumentException("Compute block sources must not be empty");
        }
        if (block.getSources().size() != 1) {
            throw new IllegalArgumentException(
                    "Partitioned compute blocks require exactly one source, found " + block.getSources().size());
        }

        Map.Entry<String, SourceVO> sourceEntry = block.getSources().entrySet().iterator().next();
        String sourceName = sourceEntry.getKey();
        RunMetrics metrics = new RunMetrics(policy.mode());
        RowSource rowSource;
        try {
            AiRunMetricsScope.bind(metrics);
            rowSource = registry.createSource(sourceName, sourceEntry.getValue(), policy);
        }
        finally {
            AiRunMetricsScope.clear();
        }
        RowSchema sourceSchema = rowSource.schema();
        List<Row> sourceRows = List.copyOf(rowSource.rows());

        int totalRows = sourceRows.size();
        if (totalRows > policy.maxRowsInMemory() && policy.failOnLimitExceeded()) {
            throw new ScaleLimitExceededException(
                    "maxRowsInMemory",
                    policy.maxRowsInMemory(),
                    totalRows,
                    "SOURCE_READ",
                    sourceName);
        }
        int partitionCount = policy.partitionCount();
        List<List<Row>> partitions = RowPartitioner.partition(sourceRows, partitionCount, policy.partitionKey());
        int executedPartitions = (int) partitions.stream().filter(bucket -> !bucket.isEmpty()).count();
        metrics.setPartitionStats(partitionCount, executedPartitions);

        boolean hasSinks = block.getSinks() != null && !block.getSinks().isEmpty();
        Object sinkLock = new Object();
        AtomicReference<RowSchema> lastSchema = new AtomicReference<>();
        List<Row> mergedRows = hasSinks ? List.of() : new ArrayList<>();

        int parallelism = Math.min(partitionCount, Runtime.getRuntime().availableProcessors());
        ForkJoinPool pool = new ForkJoinPool(Math.max(1, parallelism));
        try {
            pool.invoke(new RecursiveAction() {
                @Override
                protected void compute() {
                    List<RecursiveAction> tasks = new ArrayList<>();
                    for (List<Row> partitionRows : partitions) {
                        if (partitionRows.isEmpty()) {
                            continue;
                        }
                        tasks.add(new RecursiveAction() {
                            @Override
                            protected void compute() {
                                RunMetrics partitionMetrics = new RunMetrics(policy.mode());
                                partitionMetrics.addRead(sourceName, partitionRows.size());

                                CalciteExecutionContext context = new CalciteExecutionContext()
                                        .addTable(sourceName, sourceSchema, partitionRows);
                                CalciteRowTransformer.TransformResult result =
                                        applyTransforms(block, context, registry);
                                lastSchema.compareAndSet(null, result.schema());

                                if (hasSinks) {
                                    synchronized (sinkLock) {
                                        writeSinks(block, registry, result, partitionMetrics, policy.sinkBatchSize());
                                    }
                                } else {
                                    synchronized (mergedRows) {
                                        mergedRows.addAll(result.rows());
                                    }
                                }
                                synchronized (metrics) {
                                    RunMetricsSupport.mergeInto(metrics, partitionMetrics);
                                }
                            }
                        });
                    }
                    invokeAll(tasks);
                }
            });
        } finally {
            pool.shutdown();
        }

        RowSchema schema = lastSchema.get();
        List<Row> outputRows = hasSinks ? List.of() : List.copyOf(mergedRows);
        return new TemplateV2RunResult(schema, outputRows, metrics);
    }

    private CalciteRowTransformer.TransformResult applyTransforms(
            ComputeBlockVO block,
            CalciteExecutionContext context,
            TemplateV2RuntimeRegistry registry) {
        TransformGraphVO graph = block.getTransformGraph();
        if (graph != null && graph.getNodes() != null && !graph.getNodes().isEmpty()) {
            return transformDagExecutor.execute(graph, context, registry);
        }
        if (CollectKit.isEmpty(block.getTransformers())) {
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
            RunMetrics metrics,
            int sinkBatchSize) {
        if (block.getSinks() == null || block.getSinks().isEmpty()) {
            return;
        }
        TemplateV2VO sinkTemplate = new TemplateV2VO();
        sinkTemplate.setSinks(block.getSinks());
        SinkWriteExecutor.writeSinks(rowSinkFactory, registry, sinkTemplate, result, metrics, sinkBatchSize);
    }
}
