/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable counters and warnings collected during a Template V2 run.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class RunMetrics {

    private final String executionMode;
    private long totalRowsRead;
    private long rowsWritten;
    private int peakRowsInMemory;
    private int chunksProcessed;
    private final LinkedHashMap<String, Long> rowsReadPerSource;
    private final LinkedHashMap<String, SinkWriteMetric> sinkMetrics;
    private final ArrayList<String> warnings;
    private int configuredPartitions;
    private int executedPartitions;

    /**
     * Creates metrics for a run with the given execution mode.
     *
     * @param executionMode resolved execution mode name
     */
    public RunMetrics(String executionMode) {
        this.executionMode = executionMode;
        this.rowsReadPerSource = new LinkedHashMap<>();
        this.sinkMetrics = new LinkedHashMap<>();
        this.warnings = new ArrayList<>();
    }

    /**
     * Records rows read from a named source.
     *
     * @param sourceName source identifier
     * @param count number of rows read in this increment
     */
    public void addRead(String sourceName, int count) {
        totalRowsRead += count;
        rowsReadPerSource.merge(sourceName, (long) count, Long::sum);
    }

    /**
     * Increments the number of source chunks processed.
     */
    public void incrementChunks() {
        chunksProcessed++;
    }

    /**
     * Records rows written to sinks during this increment.
     *
     * @param count number of rows written
     */
    public void addRowsWritten(int count) {
        rowsWritten += count;
    }

    /**
     * Updates peak concurrent rows held in memory when {@code current} exceeds the prior peak.
     *
     * @param current rows currently in memory for the active chunk
     */
    public void recordPeakRowsInMemory(int current) {
        if (current > peakRowsInMemory) {
            peakRowsInMemory = current;
        }
    }

    /**
     * Resolved execution mode for this run.
     *
     * @return execution mode name
     */
    public String getExecutionMode() {
        return executionMode;
    }

    /**
     * Total rows read across all sources.
     *
     * @return cumulative row count
     */
    public long getTotalRowsRead() {
        return totalRowsRead;
    }

    /**
     * Number of source chunks processed.
     *
     * @return chunk count
     */
    public int getChunksProcessed() {
        return chunksProcessed;
    }

    /**
     * Total rows written to all sinks.
     *
     * @return cumulative rows written
     */
    public long getRowsWritten() {
        return rowsWritten;
    }

    /**
     * Maximum rows held in memory at any point during the run.
     *
     * @return peak in-memory row count
     */
    public int getPeakRowsInMemory() {
        return peakRowsInMemory;
    }

    /**
     * Per-source row read counts, in insertion order.
     *
     * @return map of source name to rows read
     */
    public Map<String, Long> getRowsReadPerSource() {
        return rowsReadPerSource;
    }

    /**
     * Non-fatal warnings collected during the run.
     *
     * @return warning messages
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Appends a diagnostic or log message to the run report warning stream.
     *
     * @param message human-readable diagnostic text
     */
    public void addWarning(String message) {
        if (message != null && !message.isBlank()) {
            warnings.add(message);
        }
    }

    /**
     * Per-sink write counters collected when {@code CONTINUE_ON_ERROR} is active.
     *
     * @return sink metric map keyed by {@code sink[index].writer[index]}
     */
    public Map<String, SinkWriteMetric> getSinkMetrics() {
        return sinkMetrics;
    }

    /**
     * Records successfully written rows for a sink writer under continue-on-error policy.
     *
     * @param sinkKey sink metric key
     * @param count number of rows written
     */
    public void recordSinkRowsOk(String sinkKey, long count) {
        sinkMetrics.computeIfAbsent(sinkKey, ignored -> new SinkWriteMetric()).addRowsOk(count);
    }

    /**
     * Records failed rows and the latest error sample for a sink writer under continue-on-error policy.
     *
     * @param sinkKey sink metric key
     * @param count number of rows that failed
     * @param errorSample error message sample
     */
    public void recordSinkRowsFailed(String sinkKey, long count, String errorSample) {
        sinkMetrics.computeIfAbsent(sinkKey, ignored -> new SinkWriteMetric()).addRowsFailed(count, errorSample);
    }

    /**
     * Sets partitioned execution counters for operator run reports.
     *
     * @param configured number of configured partitions
     * @param executed   number of partitions that processed at least one row
     */
    public void setPartitionStats(int configured, int executed) {
        this.configuredPartitions = configured;
        this.executedPartitions = executed;
    }

    /**
     * Configured partition count when partitioned execution was active.
     *
     * @return configured partitions, or {@code 0} when not partitioned
     */
    public int getConfiguredPartitions() {
        return configuredPartitions;
    }

    /**
     * Number of partitions that executed work during the run.
     *
     * @return executed partition count
     */
    public int getExecutedPartitions() {
        return executedPartitions;
    }
}
