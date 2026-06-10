/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.RowSink;
import org.gensokyo.data.calcite.sink.JdbcRowSinkAdapter;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared sink write orchestration: retry-aware JDBC writes and partial-success metrics.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class SinkWriteExecutor {

    private SinkWriteExecutor() {
    }

    /**
     * Writes transform output to all configured sinks using the template sink execution policy.
     *
     * @param rowSinkFactory sink factory
     * @param registry runtime registry (may be null when the factory ignores it)
     * @param template template definition
     * @param result transform output
     * @param metrics run metrics collector (may be null)
     * @param sinkBatchSize maximum rows per sink batch; non-positive values write all rows at once
     */
    public static void writeSinks(
            InMemoryPipeline.RowSinkFactory rowSinkFactory,
            TemplateV2RuntimeRegistry registry,
            TemplateV2VO template,
            CalciteRowTransformer.TransformResult result,
            RunMetrics metrics,
            int sinkBatchSize) {
        SinkExecutionPolicyVO policy = template.getSinkExecutionPolicy();
        SinkPolicyMode mode = sinkPolicyMode(policy);
        boolean parallelSinks = policy != null && Boolean.TRUE.equals(policy.getParallelSinks());
        int rowCount = result.rows().size();
        List<SinkWriteJob> jobs = collectSinkWriteJobs(template);
        if (parallelSinks && jobs.size() > 1) {
            Object metricsLock = metrics == null ? null : new Object();
            jobs.parallelStream().forEach(job -> executeSinkWriteJob(
                    rowSinkFactory,
                    registry,
                    policy,
                    result,
                    metrics,
                    metricsLock,
                    sinkBatchSize,
                    mode,
                    rowCount,
                    job));
            return;
        }
        for (SinkWriteJob job : jobs) {
            executeSinkWriteJob(
                    rowSinkFactory,
                    registry,
                    policy,
                    result,
                    metrics,
                    null,
                    sinkBatchSize,
                    mode,
                    rowCount,
                    job);
        }
    }

    private static List<SinkWriteJob> collectSinkWriteJobs(TemplateV2VO template) {
        List<SinkWriteJob> jobs = new ArrayList<>();
        if (template.getSinks() == null) {
            return jobs;
        }
        int sinkIndex = 0;
        for (WriteStageVO sink : template.getSinks()) {
            if (sink.getWriters() == null) {
                sinkIndex++;
                continue;
            }
            int writerIndex = 0;
            for (WriterVO writer : sink.getWriters()) {
                jobs.add(new SinkWriteJob(sinkIndex, writerIndex, writer, sinkMetricKey(sinkIndex, writerIndex)));
                writerIndex++;
            }
            sinkIndex++;
        }
        return jobs;
    }

    private static void executeSinkWriteJob(
            InMemoryPipeline.RowSinkFactory rowSinkFactory,
            TemplateV2RuntimeRegistry registry,
            SinkExecutionPolicyVO policy,
            CalciteRowTransformer.TransformResult result,
            RunMetrics metrics,
            Object metricsLock,
            int sinkBatchSize,
            SinkPolicyMode mode,
            int rowCount,
            SinkWriteJob job) {
        RowSink rowSink = prepareSink(rowSinkFactory.create(registry, job.writer()), policy);
        try {
            writeRows(rowSink, result.schema(), result.rows(), sinkBatchSize);
            if (mode == SinkPolicyMode.CONTINUE_ON_ERROR && metrics != null) {
                synchronizedMetric(metricsLock, () -> metrics.recordSinkRowsOk(job.sinkKey(), rowCount));
            }
            if (metrics != null) {
                synchronizedMetric(metricsLock, () -> metrics.addRowsWritten(rowCount));
            }
        } catch (RuntimeException ex) {
            if (mode == SinkPolicyMode.CONTINUE_ON_ERROR) {
                if (metrics != null) {
                    synchronizedMetric(metricsLock, () ->
                            metrics.recordSinkRowsFailed(job.sinkKey(), rowCount, errorMessage(ex)));
                }
            } else {
                throw sinkWriteFailure(job.sinkIndex(), job.writerIndex(), job.writer(), ex);
            }
        }
    }

    private static void synchronizedMetric(Object metricsLock, Runnable action) {
        if (metricsLock == null) {
            action.run();
            return;
        }
        synchronized (metricsLock) {
            action.run();
        }
    }

    private static void writeRows(
            RowSink rowSink,
            RowSchema schema,
            java.util.List<org.gensokyo.data.model.v2.Row> rows,
            int sinkBatchSize) {
        if (sinkBatchSize > 0) {
            rowSink.writeBatch(schema, rows, sinkBatchSize);
        } else {
            rowSink.write(schema, rows);
        }
    }

    private static RowSink prepareSink(RowSink sink, SinkExecutionPolicyVO policy) {
        if (sink instanceof JdbcRowSinkAdapter jdbcSink) {
            return jdbcSink.withRetryPolicy(policy);
        }
        return sink;
    }

    private static String sinkMetricKey(int sinkIndex, int writerIndex) {
        return "sink[" + sinkIndex + "].writer[" + writerIndex + "]";
    }

    private static String errorMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return ex.getClass().getName();
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

    private record SinkWriteJob(int sinkIndex, int writerIndex, WriterVO writer, String sinkKey) {
    }

    private enum SinkPolicyMode {
        FAIL_FAST,
        CONTINUE_ON_ERROR
    }
}
