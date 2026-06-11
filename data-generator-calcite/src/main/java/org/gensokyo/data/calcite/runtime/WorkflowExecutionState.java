/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable per-run workflow state: pause checkpoint, shared scopes, diagnostics, and last block output.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class WorkflowExecutionState {

    private final String runId;
    private int nextStepIndex;
    private final Map<String, ConcurrentHashMap<String, Object>> sharedScopes;
    private final List<String> diagnostics;
    private RowSchema lastSchema;
    private List<Row> lastRows;
    private TemplateV2RunResult lastBlockResult;

    /**
     * Creates workflow state for a new run.
     *
     * @param runId optional run identifier used for shared scope isolation
     */
    public WorkflowExecutionState(String runId) {
        this.runId = runId == null ? "workflow-run" : runId;
        this.sharedScopes = new ConcurrentHashMap<>();
        this.diagnostics = new ArrayList<>();
    }

    /**
     * Returns the workflow run identifier.
     *
     * @return run id
     */
    public String getRunId() {
        return runId;
    }

    /**
     * Returns the next step index to execute when resuming from a pause checkpoint.
     *
     * @return zero-based step index
     */
    public int getNextStepIndex() {
        return nextStepIndex;
    }

    /**
     * Updates the pause checkpoint step index.
     *
     * @param nextStepIndex zero-based step index for resume
     */
    public void setNextStepIndex(int nextStepIndex) {
        this.nextStepIndex = nextStepIndex;
    }

    /**
     * Returns shared scope maps keyed by scope id.
     *
     * @return live shared scope registry
     */
    public Map<String, ConcurrentHashMap<String, Object>> getSharedScopes() {
        return sharedScopes;
    }

    /**
     * Returns structured log and diagnostic entries collected during the run.
     *
     * @return mutable diagnostic list
     */
    public List<String> getDiagnostics() {
        return diagnostics;
    }

    /**
     * Records the most recent compute block output schema.
     *
     * @param lastSchema output schema from the last block
     */
    public void setLastSchema(RowSchema lastSchema) {
        this.lastSchema = lastSchema;
    }

    /**
     * Returns the most recent compute block output schema.
     *
     * @return last schema, or {@code null} when no block has run
     */
    public RowSchema getLastSchema() {
        return lastSchema;
    }

    /**
     * Records the most recent compute block output rows.
     *
     * @param lastRows output rows from the last block
     */
    public void setLastRows(List<Row> lastRows) {
        this.lastRows = lastRows;
    }

    /**
     * Returns the most recent compute block output rows.
     *
     * @return last rows, or {@code null} when no block has run
     */
    public List<Row> getLastRows() {
        return lastRows;
    }

    /**
     * Records the full result from the most recent compute block invocation.
     *
     * @param lastBlockResult block run result
     */
    public void setLastBlockResult(TemplateV2RunResult lastBlockResult) {
        this.lastBlockResult = lastBlockResult;
        if (lastBlockResult != null) {
            this.lastSchema = lastBlockResult.getSchema();
            this.lastRows = lastBlockResult.getRows();
        }
    }

    /**
     * Returns the full result from the most recent compute block invocation.
     *
     * @return last block result, or {@code null} when no block has run
     */
    public TemplateV2RunResult getLastBlockResult() {
        return lastBlockResult;
    }
}
