/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.model.v2.ExecutionPolicyVO;

import java.util.Locale;

/**
 * Resolved execution policy for Template V2 runtime, combining repository defaults with template overrides.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class EffectiveExecutionPolicy {

    private static final String DEFAULT_MODE = "IN_MEMORY";
    private static final int DEFAULT_MAX_ROWS_IN_MEMORY = 500_000;
    private static final int DEFAULT_SOURCE_CHUNK_SIZE = 5_000;
    private static final int DEFAULT_SINK_BATCH_SIZE = 1_000;
    private static final int DEFAULT_PREVIEW_ROW_LIMIT = 100;
    private static final boolean DEFAULT_FAIL_ON_LIMIT_EXCEEDED = true;
    private static final int BROADCAST_MAX_ROWS_CAP = 50_000;

    private final String mode;
    private final int maxRowsInMemory;
    private final int sourceChunkSize;
    private final int sinkBatchSize;
    private final int previewRowLimit;
    private final boolean failOnLimitExceeded;
    private final int broadcastMaxRows;

    private EffectiveExecutionPolicy(
            String mode,
            int maxRowsInMemory,
            int sourceChunkSize,
            int sinkBatchSize,
            int previewRowLimit,
            boolean failOnLimitExceeded,
            int broadcastMaxRows) {
        this.mode = mode;
        this.maxRowsInMemory = maxRowsInMemory;
        this.sourceChunkSize = sourceChunkSize;
        this.sinkBatchSize = sinkBatchSize;
        this.previewRowLimit = previewRowLimit;
        this.failOnLimitExceeded = failOnLimitExceeded;
        this.broadcastMaxRows = broadcastMaxRows;
    }

    /**
     * Resolves effective execution policy from template policy, applying repository defaults for unset fields.
     *
     * @param templatePolicy optional template execution policy; may be {@code null}
     * @return immutable effective policy
     */
    public static EffectiveExecutionPolicy resolve(ExecutionPolicyVO templatePolicy) {
        String mode = DEFAULT_MODE;
        int maxRowsInMemory = DEFAULT_MAX_ROWS_IN_MEMORY;
        int sourceChunkSize = DEFAULT_SOURCE_CHUNK_SIZE;
        int sinkBatchSize = DEFAULT_SINK_BATCH_SIZE;
        int previewRowLimit = DEFAULT_PREVIEW_ROW_LIMIT;
        boolean failOnLimitExceeded = DEFAULT_FAIL_ON_LIMIT_EXCEEDED;

        if (templatePolicy != null) {
            if (templatePolicy.getMode() != null && !templatePolicy.getMode().isBlank()) {
                mode = templatePolicy.getMode().trim().toUpperCase(Locale.ROOT);
            }
            if (templatePolicy.getMaxRowsInMemory() != null) {
                maxRowsInMemory = templatePolicy.getMaxRowsInMemory();
            }
            if (templatePolicy.getSourceChunkSize() != null) {
                sourceChunkSize = templatePolicy.getSourceChunkSize();
            }
            if (templatePolicy.getSinkBatchSize() != null) {
                sinkBatchSize = templatePolicy.getSinkBatchSize();
            }
            if (templatePolicy.getPreviewRowLimit() != null) {
                previewRowLimit = templatePolicy.getPreviewRowLimit();
            }
            if (templatePolicy.getFailOnLimitExceeded() != null) {
                failOnLimitExceeded = templatePolicy.getFailOnLimitExceeded();
            }
        }

        int broadcastMaxRows;
        if (templatePolicy != null && templatePolicy.getBroadcastMaxRows() != null) {
            broadcastMaxRows = templatePolicy.getBroadcastMaxRows();
        } else {
            broadcastMaxRows = Math.min(BROADCAST_MAX_ROWS_CAP, maxRowsInMemory / 10);
        }
        return new EffectiveExecutionPolicy(
                mode,
                maxRowsInMemory,
                sourceChunkSize,
                sinkBatchSize,
                previewRowLimit,
                failOnLimitExceeded,
                broadcastMaxRows);
    }

    /**
     * Execution mode (normalized uppercase).
     *
     * @return mode name
     */
    public String mode() {
        return mode;
    }

    /**
     * Maximum rows allowed in memory for the run.
     *
     * @return row limit
     */
    public int maxRowsInMemory() {
        return maxRowsInMemory;
    }

    /**
     * JDBC fetch / source chunk size.
     *
     * @return chunk size
     */
    public int sourceChunkSize() {
        return sourceChunkSize;
    }

    /**
     * Sink batch size for JDBC, Kafka, Elasticsearch, etc.
     *
     * @return batch size
     */
    public int sinkBatchSize() {
        return sinkBatchSize;
    }

    /**
     * Row limit for preview and analyze APIs.
     *
     * @return preview limit
     */
    public int previewRowLimit() {
        return previewRowLimit;
    }

    /**
     * Whether the run should fail when a configured limit is exceeded.
     *
     * @return {@code true} to fail on limit exceeded
     */
    public boolean failOnLimitExceeded() {
        return failOnLimitExceeded;
    }

    /**
     * Maximum rows for broadcast dimension materialization.
     *
     * @return broadcast row cap
     */
    public int broadcastMaxRows() {
        return broadcastMaxRows;
    }
}
