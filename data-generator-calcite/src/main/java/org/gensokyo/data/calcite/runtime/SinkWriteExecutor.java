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
        int rowCount = result.rows().size();
        int sinkIndex = 0;
        for (WriteStageVO sink : template.getSinks()) {
            int writerIndex = 0;
            for (WriterVO writer : sink.getWriters()) {
                String sinkKey = sinkMetricKey(sinkIndex, writerIndex);
                RowSink rowSink = prepareSink(rowSinkFactory.create(registry, writer), policy);
                try {
                    writeRows(rowSink, result.schema(), result.rows(), sinkBatchSize);
                    if (mode == SinkPolicyMode.CONTINUE_ON_ERROR && metrics != null) {
                        metrics.recordSinkRowsOk(sinkKey, rowCount);
                    }
                    if (metrics != null) {
                        metrics.addRowsWritten(rowCount);
                    }
                } catch (RuntimeException ex) {
                    if (mode == SinkPolicyMode.CONTINUE_ON_ERROR) {
                        if (metrics != null) {
                            metrics.recordSinkRowsFailed(sinkKey, rowCount, errorMessage(ex));
                        }
                    } else {
                        throw sinkWriteFailure(sinkIndex, writerIndex, writer, ex);
                    }
                }
                writerIndex++;
            }
            sinkIndex++;
        }
    }

    private static void writeRows(RowSink rowSink, RowSchema schema, java.util.List<org.gensokyo.data.model.v2.Row> rows, int sinkBatchSize) {
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

    private enum SinkPolicyMode {
        FAIL_FAST,
        CONTINUE_ON_ERROR
    }
}
