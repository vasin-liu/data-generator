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
    private int chunksProcessed;
    private final LinkedHashMap<String, Long> rowsReadPerSource;
    private final ArrayList<String> warnings;

    /**
     * Creates metrics for a run with the given execution mode.
     *
     * @param executionMode resolved execution mode name
     */
    public RunMetrics(String executionMode) {
        this.executionMode = executionMode;
        this.rowsReadPerSource = new LinkedHashMap<>();
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
}
