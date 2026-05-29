/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.workflow.BranchStepVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.model.v2.workflow.InvokeComputeBlockStepVO;
import org.gensokyo.data.model.v2.workflow.LogStepVO;
import org.gensokyo.data.model.v2.workflow.PauseStepVO;
import org.gensokyo.data.model.v2.workflow.SharedScopeStepVO;
import org.gensokyo.data.model.v2.workflow.WorkflowSpecVO;
import org.gensokyo.data.model.v2.workflow.WorkflowStepVO;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Executes L2 workflow steps: pause, log, branch, shared scope, and compute block invocation.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class WorkflowRunner {

    private static final ExpressionParser SPEL = new SpelExpressionParser();
    private static final long CONDITION_POLL_INTERVAL_MS = 50L;

    private final ComputeBlockRunner computeBlockRunner;

    /**
     * Creates a runner that resolves sinks through the runtime registry.
     */
    public WorkflowRunner() {
        this((registry, writer) -> registry.createSink(writer));
    }

    /**
     * Creates a runner with a custom sink factory (typical in tests).
     *
     * @param rowSinkFactory sink factory passed to compute block execution
     */
    public WorkflowRunner(InMemoryPipeline.RowSinkFactory rowSinkFactory) {
        this(new ComputeBlockRunner(rowSinkFactory));
    }

    /**
     * Creates a runner with explicit compute block collaborator.
     *
     * @param computeBlockRunner compute block executor
     */
    public WorkflowRunner(ComputeBlockRunner computeBlockRunner) {
        this.computeBlockRunner = computeBlockRunner;
    }

    /**
     * Executes the workflow attached to the template and returns aggregated metrics and last block output.
     *
     * @param template template containing workflow steps and compute blocks
     * @param policy   resolved execution policy
     * @param registry runtime registry for block execution
     * @return run result with last compute block output and merged metrics
     * @throws IllegalArgumentException when workflow or referenced compute blocks are invalid
     */
    public TemplateV2RunResult run(
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry) {
        return run(template, policy, registry, null);
    }

    /**
     * Executes the workflow with an optional diagnostic callback for log steps.
     *
     * @param template            template containing workflow steps and compute blocks
     * @param policy              resolved execution policy
     * @param registry            runtime registry for block execution
     * @param diagnosticCollector optional callback receiving each log diagnostic entry
     * @return run result with last compute block output and merged metrics
     */
    public TemplateV2RunResult run(
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry,
            Consumer<String> diagnosticCollector) {
        WorkflowSpecVO workflow = template.getWorkflow();
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow template requires workflow spec");
        }
        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            throw new IllegalArgumentException("Workflow steps must not be empty");
        }

        Map<String, ComputeBlockVO> blocksById = indexComputeBlocks(template.getComputeBlocks());
        String runId = template.getInstanceId() != null
                ? String.valueOf(template.getInstanceId())
                : template.getName();
        WorkflowExecutionState state = new WorkflowExecutionState(runId);
        RunMetrics metrics = new RunMetrics(policy.mode());

        List<WorkflowStepVO> steps = workflow.getSteps();
        for (int index = state.getNextStepIndex(); index < steps.size(); index++) {
            state.setNextStepIndex(index + 1);
            executeStep(steps.get(index), template, policy, registry, blocksById, state, metrics, diagnosticCollector);
        }

        mergeDiagnosticsIntoMetrics(state, metrics);
        RowSchema schema = state.getLastSchema() == null ? new RowSchema() : state.getLastSchema();
        List<Row> rows = state.getLastRows() == null ? List.of() : state.getLastRows();
        return new TemplateV2RunResult(schema, rows, metrics);
    }

    private void executeStep(
            WorkflowStepVO step,
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry,
            Map<String, ComputeBlockVO> blocksById,
            WorkflowExecutionState state,
            RunMetrics metrics,
            Consumer<String> diagnosticCollector) {
        if (step instanceof LogStepVO logStep) {
            executeLogStep(logStep, state, metrics, diagnosticCollector);
            return;
        }
        if (step instanceof PauseStepVO pauseStep) {
            executePauseStep(pauseStep, state);
            return;
        }
        if (step instanceof InvokeComputeBlockStepVO invokeStep) {
            executeInvokeComputeBlockStep(invokeStep, policy, registry, blocksById, state, metrics);
            return;
        }
        if (step instanceof BranchStepVO branchStep) {
            executeBranchStep(branchStep, template, policy, registry, blocksById, state, metrics, diagnosticCollector);
            return;
        }
        if (step instanceof SharedScopeStepVO sharedScopeStep) {
            executeSharedScopeStep(sharedScopeStep, state);
            return;
        }
        throw new IllegalArgumentException("Unsupported workflow step type: " + step.getType());
    }

    private void executeLogStep(
            LogStepVO step,
            WorkflowExecutionState state,
            RunMetrics metrics,
            Consumer<String> diagnosticCollector) {
        String level = step.getLevel() == null || step.getLevel().isBlank() ? "INFO" : step.getLevel();
        String message = step.getMessage() == null ? "" : step.getMessage();
        String entry = formatLogEntry(step.getId(), level, message, step.getFields());
        state.getDiagnostics().add(entry);
        metrics.addWarning(entry);
        if (diagnosticCollector != null) {
            diagnosticCollector.accept(entry);
        }
    }

    private static String formatLogEntry(
            String stepId,
            String level,
            String message,
            Map<String, Object> fields) {
        StringBuilder builder = new StringBuilder("[LOG][")
                .append(level)
                .append("]");
        if (stepId != null && !stepId.isBlank()) {
            builder.append('[').append(stepId).append(']');
        }
        builder.append(' ').append(message);
        if (fields != null && !fields.isEmpty()) {
            builder.append(' ').append(fields);
        }
        return builder.toString();
    }

    private void executePauseStep(PauseStepVO step, WorkflowExecutionState state) {
        if (step.getDurationMs() != null && step.getDurationMs() > 0) {
            sleep(Duration.ofMillis(step.getDurationMs()));
            return;
        }
        if (step.getUntil() != null && !step.getUntil().isBlank()) {
            Instant deadline = Instant.parse(step.getUntil());
            long remainingMs = Duration.between(Instant.now(), deadline).toMillis();
            if (remainingMs > 0) {
                sleep(Duration.ofMillis(remainingMs));
            }
            return;
        }
        if (step.getCondition() != null && !step.getCondition().isBlank()) {
            waitUntilCondition(step.getCondition(), state);
            return;
        }
        throw new IllegalArgumentException("Pause step requires durationMs, until, or condition");
    }

    private void waitUntilCondition(String condition, WorkflowExecutionState state) {
        Expression expression = SPEL.parseExpression(condition);
        StandardEvaluationContext context = buildSpelContext(state);
        while (!Boolean.TRUE.equals(expression.getValue(context, Boolean.class))) {
            sleep(Duration.ofMillis(CONDITION_POLL_INTERVAL_MS));
        }
    }

    private void executeInvokeComputeBlockStep(
            InvokeComputeBlockStepVO step,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry,
            Map<String, ComputeBlockVO> blocksById,
            WorkflowExecutionState state,
            RunMetrics metrics) {
        String blockId = step.getComputeBlockId();
        if (blockId == null || blockId.isBlank()) {
            throw new IllegalArgumentException("Invoke compute block step requires computeBlockId");
        }
        ComputeBlockVO block = blocksById.get(blockId);
        if (block == null) {
            throw new IllegalArgumentException("Unknown compute block id: " + blockId);
        }
        attachSharedScope(block, state);
        TemplateV2RunResult blockResult = computeBlockRunner.run(block, policy, registry);
        state.setLastBlockResult(blockResult);
        mergeBlockMetrics(blockResult, metrics);
    }

    private void executeBranchStep(
            BranchStepVO step,
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry,
            Map<String, ComputeBlockVO> blocksById,
            WorkflowExecutionState state,
            RunMetrics metrics,
            Consumer<String> diagnosticCollector) {
        if (step.getCondition() == null || step.getCondition().isBlank()) {
            throw new IllegalArgumentException("Branch step requires condition");
        }
        Expression expression = SPEL.parseExpression(step.getCondition());
        StandardEvaluationContext context = buildSpelContext(state);
        boolean branchTaken = Boolean.TRUE.equals(expression.getValue(context, Boolean.class));

        if (branchTaken) {
            if (step.getThenComputeBlockId() != null && !step.getThenComputeBlockId().isBlank()) {
                InvokeComputeBlockStepVO invoke = new InvokeComputeBlockStepVO();
                invoke.setComputeBlockId(step.getThenComputeBlockId());
                executeInvokeComputeBlockStep(invoke, policy, registry, blocksById, state, metrics);
                return;
            }
            executeSteps(step.getThenSteps(), template, policy, registry, blocksById, state, metrics, diagnosticCollector);
            return;
        }
        executeSteps(step.getElseSteps(), template, policy, registry, blocksById, state, metrics, diagnosticCollector);
    }

    private void executeSteps(
            List<WorkflowStepVO> steps,
            TemplateV2VO template,
            EffectiveExecutionPolicy policy,
            TemplateV2RuntimeRegistry registry,
            Map<String, ComputeBlockVO> blocksById,
            WorkflowExecutionState state,
            RunMetrics metrics,
            Consumer<String> diagnosticCollector) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        for (WorkflowStepVO nested : steps) {
            executeStep(nested, template, policy, registry, blocksById, state, metrics, diagnosticCollector);
        }
    }

    private void executeSharedScopeStep(SharedScopeStepVO step, WorkflowExecutionState state) {
        String scopeId = step.getScopeId();
        if (scopeId == null || scopeId.isBlank()) {
            throw new IllegalArgumentException("Shared scope step requires scopeId");
        }
        String action = step.getAction() == null ? "" : step.getAction().trim().toLowerCase();
        switch (action) {
            case "open" -> state.getSharedScopes().computeIfAbsent(scopeId, ignored -> new ConcurrentHashMap<>());
            case "write" -> {
                ConcurrentHashMap<String, Object> scope = requireSharedScope(scopeId, state);
                if (step.getEntries() != null) {
                    scope.putAll(step.getEntries());
                }
            }
            case "read" -> requireSharedScope(scopeId, state);
            case "close" -> state.getSharedScopes().remove(scopeId);
            default -> throw new IllegalArgumentException("Unsupported shared scope action: " + step.getAction());
        }
    }

    private static ConcurrentHashMap<String, Object> requireSharedScope(String scopeId, WorkflowExecutionState state) {
        ConcurrentHashMap<String, Object> scope = state.getSharedScopes().get(scopeId);
        if (scope == null) {
            throw new IllegalArgumentException("Shared scope is not open: " + scopeId);
        }
        return scope;
    }

    private static void attachSharedScope(ComputeBlockVO block, WorkflowExecutionState state) {
        if (block.getSharedScopeId() == null || block.getSharedScopeId().isBlank()) {
            return;
        }
        state.getSharedScopes().computeIfAbsent(block.getSharedScopeId(), ignored -> new ConcurrentHashMap<>());
    }

    private static StandardEvaluationContext buildSpelContext(WorkflowExecutionState state) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.addPropertyAccessor(new MapAccessor());
        Map<String, Object> shared = new LinkedHashMap<>();
        state.getSharedScopes().forEach(shared::put);
        context.setVariable("shared", shared);
        context.setVariable("scopes", state.getSharedScopes());
        context.setVariable("runId", state.getRunId());
        return context;
    }

    private static Map<String, ComputeBlockVO> indexComputeBlocks(List<ComputeBlockVO> computeBlocks) {
        Map<String, ComputeBlockVO> blocksById = new LinkedHashMap<>();
        if (computeBlocks == null) {
            return blocksById;
        }
        for (ComputeBlockVO block : computeBlocks) {
            if (block == null || block.getId() == null || block.getId().isBlank()) {
                throw new IllegalArgumentException("Compute block requires non-blank id");
            }
            blocksById.put(block.getId(), block);
        }
        return blocksById;
    }

    private static void mergeBlockMetrics(TemplateV2RunResult blockResult, RunMetrics metrics) {
        if (blockResult == null || blockResult.getMetrics() == null) {
            return;
        }
        RunMetrics blockMetrics = blockResult.getMetrics();
        blockMetrics.getRowsReadPerSource().forEach((source, count) -> metrics.addRead(source, count.intValue()));
        metrics.addRowsWritten((int) blockMetrics.getRowsWritten());
        metrics.recordPeakRowsInMemory(blockMetrics.getPeakRowsInMemory());
        for (int chunk = 0; chunk < blockMetrics.getChunksProcessed(); chunk++) {
            metrics.incrementChunks();
        }
        blockMetrics.getWarnings().forEach(metrics::addWarning);
    }

    private static void mergeDiagnosticsIntoMetrics(WorkflowExecutionState state, RunMetrics metrics) {
        for (String diagnostic : state.getDiagnostics()) {
            if (!metrics.getWarnings().contains(diagnostic)) {
                metrics.addWarning(diagnostic);
            }
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Workflow pause interrupted", interrupted);
        }
    }
}
