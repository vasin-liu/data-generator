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
    private static final int DEFAULT_PARTITION_COUNT = 1;

    private final String mode;
    private final int maxRowsInMemory;
    private final Integer maxTotalRows;
    private final int sourceChunkSize;
    private final int sinkBatchSize;
    private final int previewRowLimit;
    private final boolean failOnLimitExceeded;
    private final int broadcastMaxRows;
    private final int partitionCount;
    private final String partitionKey;

    private EffectiveExecutionPolicy(
            String mode,
            int maxRowsInMemory,
            Integer maxTotalRows,
            int sourceChunkSize,
            int sinkBatchSize,
            int previewRowLimit,
            boolean failOnLimitExceeded,
            int broadcastMaxRows,
            int partitionCount,
            String partitionKey) {
        this.mode = mode;
        this.maxRowsInMemory = maxRowsInMemory;
        this.maxTotalRows = maxTotalRows;
        this.sourceChunkSize = sourceChunkSize;
        this.sinkBatchSize = sinkBatchSize;
        this.previewRowLimit = previewRowLimit;
        this.failOnLimitExceeded = failOnLimitExceeded;
        this.broadcastMaxRows = broadcastMaxRows;
        this.partitionCount = partitionCount;
        this.partitionKey = partitionKey;
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
        Integer maxTotalRows = null;
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
            if (templatePolicy.getMaxTotalRows() != null) {
                maxTotalRows = templatePolicy.getMaxTotalRows();
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
            } else if (maxTotalRows != null) {
                // maxTotalRows implies fail-fast when the flag is omitted.
                failOnLimitExceeded = true;
            }
        }

        int broadcastMaxRows;
        if (templatePolicy != null && templatePolicy.getBroadcastMaxRows() != null) {
            broadcastMaxRows = templatePolicy.getBroadcastMaxRows();
        } else {
            broadcastMaxRows = Math.min(BROADCAST_MAX_ROWS_CAP, maxRowsInMemory / 10);
        }

        int partitionCount = DEFAULT_PARTITION_COUNT;
        String partitionKey = null;
        if (templatePolicy != null) {
            if (templatePolicy.getPartitionCount() != null) {
                partitionCount = templatePolicy.getPartitionCount();
            }
            if (templatePolicy.getPartitionKey() != null && !templatePolicy.getPartitionKey().isBlank()) {
                partitionKey = templatePolicy.getPartitionKey().trim();
            }
        }
        return new EffectiveExecutionPolicy(
                mode,
                maxRowsInMemory,
                maxTotalRows,
                sourceChunkSize,
                sinkBatchSize,
                previewRowLimit,
                failOnLimitExceeded,
                broadcastMaxRows,
                partitionCount,
                partitionKey);
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
     * Optional cap on total rows processed in the run; {@code null} when unset.
     *
     * @return configured max total rows, or {@code null} when no cap applies
     */
    public Integer maxTotalRows() {
        return maxTotalRows;
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

    /**
     * Number of in-process partitions for compute block execution.
     *
     * @return partition count; {@code 1} disables partitioned execution
     */
    public int partitionCount() {
        return partitionCount;
    }

    /**
     * Optional column name used to hash rows into partitions.
     *
     * @return partition key column, or {@code null} for round-robin assignment
     */
    public String partitionKey() {
        return partitionKey;
    }
}
