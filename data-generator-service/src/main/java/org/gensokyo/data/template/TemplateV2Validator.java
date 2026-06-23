package org.gensokyo.data.template;

import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.calcite.runtime.TransformDagExecutor;
import org.gensokyo.data.calcite.runtime.TransformDagValidationException;
import org.gensokyo.data.calcite.sql.ExecutionShape;
import org.gensokyo.data.calcite.sql.ExecutionShapeClassifier;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.JsTransformVO;
import org.gensokyo.data.model.v2.MaterializationPolicyVO;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.SpelColumnMapping;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.v2.workflow.BranchStepVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.model.v2.workflow.InvokeComputeBlockStepVO;
import org.gensokyo.data.model.v2.workflow.LogStepVO;
import org.gensokyo.data.model.v2.workflow.PauseStepVO;
import org.gensokyo.data.model.v2.workflow.SharedScopeStepVO;
import org.gensokyo.data.model.v2.workflow.WorkflowSpecVO;
import org.gensokyo.data.model.v2.workflow.WorkflowStepVO;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Structural and semantic validation for Template V2 definitions, including workflow and transform DAG rules.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class TemplateV2Validator {
    private TemplateV2Validator() {
    }

    public static void validate(TemplateV2VO template) {
        if (template == null) {
            throw new IllegalArgumentException("Template V2 must not be null");
        }
        if (StrKit.isBlank(template.getName())) {
            throw new IllegalArgumentException("Template V2 name must not be blank");
        }

        boolean workflowMode = template.getWorkflow() != null;
        if (!workflowMode) {
            if (CollectKit.isEmpty(template.getSources())) {
                throw new IllegalArgumentException("Template V2 sources must not be empty");
            }
            if (CollectKit.isEmpty(template.getTransformers())) {
                throw new IllegalArgumentException("Template V2 transformers must not be empty");
            }
            if (CollectKit.isEmpty(template.getSinks())) {
                throw new IllegalArgumentException("Template V2 sinks must not be empty");
            }
        }

        if (workflowMode) {
            validateWorkflowTemplate(template);
        }

        validateLinearTransformers(template);
        validateExecutionPolicy(template);

        for (var entry : template.getSources().entrySet()) {
            if (StrKit.isBlank(entry.getKey())) {
                throw new IllegalArgumentException("Template V2 source name must not be blank");
            }
            if (Objects.isNull(entry.getValue())) {
                throw new IllegalArgumentException("Template V2 source '" + entry.getKey() + "' must not be null");
            }
            validateMaterializationPolicy(entry.getKey(), entry.getValue().getMaterializationPolicy());
            if (entry.getValue() instanceof InlineRowsSourceVO inlineRows) {
                validateInlineRowsSource(entry.getKey(), inlineRows);
            }
        }

        if (!workflowMode) {
            for (var sink : template.getSinks()) {
                if (sink == null || CollectKit.isEmpty(sink.getWriters())) {
                    throw new IllegalArgumentException("Template V2 sink must contain at least one writer");
                }
            }
        }

        validateSinkExecutionPolicy(template.getSinkExecutionPolicy());
        validateComputeBlocks(template);
    }

    /**
     * Validates governance rules that require Spring configuration (plaintext secrets).
     *
     * @param template normalized template
     * @param rejectPlaintextPasswords when true, inline plaintext passwords are errors
     */
    public static void validateGovernance(TemplateV2VO template, boolean rejectPlaintextPasswords) {
        for (String error : TemplateGovernanceSupport.collectSecretViolations(template, rejectPlaintextPasswords)) {
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * Collects non-fatal validation warnings for a normalized Template V2 definition.
     *
     * @param template normalized template
     * @return warning messages; never {@code null}
     */
    public static List<String> collectWarnings(TemplateV2VO template) {
        List<String> warnings = new ArrayList<>();
        if (template == null) {
            return warnings;
        }
        appendMaxTotalRowsWarnings(template, warnings);
        return warnings;
    }

    private static void appendMaxTotalRowsWarnings(TemplateV2VO template, List<String> warnings) {
        ExecutionPolicyVO policy = template.getExecutionPolicy();
        if (policy == null || StrKit.isBlank(policy.getMode())) {
            return;
        }
        String mode = policy.getMode().trim().toUpperCase(Locale.ROOT);
        if (("CHUNKED".equals(mode) || "STREAMING".equals(mode)) && policy.getMaxTotalRows() == null) {
            warnings.add("CHUNKED/STREAMING mode without maxTotalRows — consider setting a row cap for fail-safe execution");
        }
    }

    private static void validateLinearTransformers(TemplateV2VO template) {
        if (CollectKit.isEmpty(template.getTransformers())) {
            return;
        }
        var names = new HashSet<String>();
        boolean requireNames = template.getTransformers().size() > 1;
        for (int index = 0; index < template.getTransformers().size(); index++) {
            TransformVO transformer = template.getTransformers().get(index);
            String path = "transformers[" + index + "]";
            if (transformer == null) {
                throw new IllegalArgumentException(pathMessage(path, "Template V2 transformer must not be null"));
            }
            if (requireNames && StrKit.isBlank(transformer.getName())) {
                throw new IllegalArgumentException(pathMessage(path,
                        "Template V2 transformers must all be named when more than one transformer is configured"));
            }
            if (StrKit.isNotBlank(transformer.getName()) && !names.add(transformer.getName())) {
                throw new IllegalArgumentException(pathMessage(path, "Duplicate transformer name: " + transformer.getName()));
            }
            validateTransform(transformer, path);
        }
    }

    private static void validateWorkflowTemplate(TemplateV2VO template) {
        if (!CollectKit.isEmpty(template.getTransformers()) && CollectKit.isEmpty(template.getComputeBlocks())) {
            throw new IllegalArgumentException(pathMessage("transformers",
                    "workflow templates with top-level transformers require computeBlocks"));
        }

        WorkflowSpecVO workflow = template.getWorkflow();
        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            throw new IllegalArgumentException(pathMessage("workflow.steps", "workflow steps must not be empty"));
        }
        for (int index = 0; index < workflow.getSteps().size(); index++) {
            validateWorkflowStep(workflow.getSteps().get(index), "workflow.steps[" + index + "]");
        }
    }

    private static void validateWorkflowStep(WorkflowStepVO step, String path) {
        if (step == null) {
            throw new IllegalArgumentException(pathMessage(path, "workflow step must not be null"));
        }
        if (StrKit.isBlank(step.getType())) {
            throw new IllegalArgumentException(pathMessage(path + ".type", "workflow step type must not be blank"));
        }
        if (!(step instanceof PauseStepVO
                || step instanceof LogStepVO
                || step instanceof BranchStepVO
                || step instanceof SharedScopeStepVO
                || step instanceof InvokeComputeBlockStepVO)) {
            throw new IllegalArgumentException(pathMessage(path + ".type",
                    "unsupported workflow step type: " + step.getType()));
        }
        if (step instanceof BranchStepVO branchStep) {
            validateNestedWorkflowSteps(branchStep.getThenSteps(), path + ".thenSteps");
            validateNestedWorkflowSteps(branchStep.getElseSteps(), path + ".elseSteps");
        }
    }

    private static void validateNestedWorkflowSteps(List<WorkflowStepVO> steps, String pathPrefix) {
        if (steps == null) {
            return;
        }
        for (int index = 0; index < steps.size(); index++) {
            validateWorkflowStep(steps.get(index), pathPrefix + "[" + index + "]");
        }
    }

    private static void validateComputeBlocks(TemplateV2VO template) {
        if (CollectKit.isEmpty(template.getComputeBlocks())) {
            return;
        }
        for (int blockIndex = 0; blockIndex < template.getComputeBlocks().size(); blockIndex++) {
            ComputeBlockVO block = template.getComputeBlocks().get(blockIndex);
            String blockPath = "computeBlocks[" + blockIndex + "]";
            if (block == null) {
                throw new IllegalArgumentException(pathMessage(blockPath, "compute block must not be null"));
            }
            if (StrKit.isBlank(block.getId())) {
                throw new IllegalArgumentException(pathMessage(blockPath + ".id", "compute block id must not be blank"));
            }
            if (!CollectKit.isEmpty(block.getTransformers())) {
                for (int index = 0; index < block.getTransformers().size(); index++) {
                    TransformVO transformer = block.getTransformers().get(index);
                    validateTransform(transformer, blockPath + ".transformers[" + index + "]");
                }
            }
            TransformGraphVO graph = block.getTransformGraph();
            if (graph != null && graph.getNodes() != null && !graph.getNodes().isEmpty()) {
                validateTransformGraph(graph, blockPath + ".transformGraph");
            }
        }
    }

    private static void validateTransformGraph(TransformGraphVO graph, String pathPrefix) {
        try {
            new TransformDagExecutor().topologicalSort(graph);
        }
        catch (TransformDagValidationException exception) {
            throw new IllegalArgumentException(pathMessage(pathPrefix, exception.getMessage()));
        }
        if (graph.getTransforms() == null) {
            return;
        }
        for (var entry : graph.getTransforms().entrySet()) {
            validateTransform(entry.getValue(), pathPrefix + ".transforms." + entry.getKey());
        }
    }

    private static void validateTransform(TransformVO transformer, String path) {
        if (transformer == null) {
            throw new IllegalArgumentException(pathMessage(path, "transform must not be null"));
        }
        if (transformer instanceof SqlTransformVO sqlTransform && StrKit.isBlank(sqlTransform.getSql())) {
            throw new IllegalArgumentException(pathMessage(path + ".sql", "SQL transformer SQL must not be blank"));
        }
        if (transformer instanceof SpelTransformVO spelTransform) {
            validateSpelTransform(spelTransform, path);
        }
        if (transformer instanceof JsTransformVO jsTransform) {
            validateJsTransform(jsTransform, path);
        }
    }

    private static String pathMessage(String path, String message) {
        return path + ": " + message;
    }

    private static void validateJsTransform(JsTransformVO jsTransform, String path) {
        if (StrKit.isBlank(jsTransform.getScript())) {
            throw new IllegalArgumentException(pathMessage(path + ".script", "JS transformer script must not be blank"));
        }
        if (jsTransform.getScript().getBytes(StandardCharsets.UTF_8).length > JsTransformVO.MAX_SCRIPT_BYTES) {
            throw new IllegalArgumentException(pathMessage(path + ".script", "JS transformer script exceeds maximum size of "
                    + JsTransformVO.MAX_SCRIPT_BYTES + " bytes"));
        }
        if (jsTransform.getTimeoutMs() != null && jsTransform.getTimeoutMs() <= 0) {
            throw new IllegalArgumentException(pathMessage(path + ".timeoutMs",
                    "JS transformer timeoutMs must be positive when set"));
        }
    }

    private static void validateSpelTransform(SpelTransformVO spelTransform, String path) {
        if (CollectKit.isEmpty(spelTransform.getColumns())) {
            throw new IllegalArgumentException(pathMessage(path + ".columns", "SpEL transformer columns must not be empty"));
        }
        for (int index = 0; index < spelTransform.getColumns().size(); index++) {
            SpelColumnMapping column = spelTransform.getColumns().get(index);
            if (column == null || StrKit.isBlank(column.getName()) || StrKit.isBlank(column.getExpression())) {
                throw new IllegalArgumentException(pathMessage(path + ".columns[" + index + "]",
                        "SpEL transformer column name and expression must not be blank"));
            }
        }
    }

    private static void validateMaterializationPolicy(String sourceName, MaterializationPolicyVO policy) {
        if (policy == null || StrKit.isBlank(policy.getMode())) {
            return;
        }
        String mode = policy.getMode().trim().toUpperCase(Locale.ROOT);
        if (!"ORDERED".equals(mode)
                && !"LIMIT".equals(mode)
                && !"ONCE".equals(mode)
                && !"EQUAL".equals(mode)
                && !"WEIGHTED".equals(mode)) {
            throw new IllegalArgumentException("Unsupported materialization policy mode on source '"
                    + sourceName + "': " + policy.getMode());
        }
        if (policy.getLimit() != null && policy.getLimit() < 0) {
            throw new IllegalArgumentException("Materialization policy limit on source '"
                    + sourceName + "' must be greater than or equal to 0");
        }
        if ("LIMIT".equals(mode) && (policy.getLimit() == null || policy.getLimit() <= 0)) {
            throw new IllegalArgumentException("LIMIT materialization policy on source '"
                    + sourceName + "' requires a positive limit");
        }
        if ("WEIGHTED".equals(mode)) {
            if (CollectKit.isEmpty(policy.getWeights())) {
                throw new IllegalArgumentException("WEIGHTED materialization policy on source '"
                        + sourceName + "' requires non-empty weights");
            }
            for (int index = 0; index < policy.getWeights().size(); index++) {
                Integer weight = policy.getWeights().get(index);
                if (weight == null || weight <= 0) {
                    throw new IllegalArgumentException("WEIGHTED materialization policy weight at index ["
                            + index + "] on source '" + sourceName + "' must be positive");
                }
            }
        }
    }

    private static void validateInlineRowsSource(String sourceName, InlineRowsSourceVO source) {
        if (CollectKit.isEmpty(source.getRows())) {
            throw new IllegalArgumentException("Template V2 source '" + sourceName
                    + "' (inline_rows) must contain at least one row");
        }
    }

    private static void validateSinkExecutionPolicy(SinkExecutionPolicyVO policy) {
        if (policy == null || StrKit.isBlank(policy.getMode())) {
            return;
        }
        String mode = policy.getMode().trim().toUpperCase();
        if (!"FAIL_FAST".equals(mode) && !"CONTINUE_ON_ERROR".equals(mode)) {
            throw new IllegalArgumentException("Unsupported sink execution policy mode: " + policy.getMode());
        }
        if (policy.getMaxRetries() != null && policy.getMaxRetries() <= 0) {
            throw new IllegalArgumentException("Sink execution policy maxRetries must be positive");
        }
        if (policy.getRetryBackoffMs() != null && policy.getRetryBackoffMs() < 0) {
            throw new IllegalArgumentException("Sink execution policy retryBackoffMs must not be negative");
        }
    }

    private static final Pattern SQL_JOIN = Pattern.compile("\\bJOIN\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_SELECT_DISTINCT =
            Pattern.compile("\\bSELECT\\s+DISTINCT\\b", Pattern.CASE_INSENSITIVE);

    private static void validateExecutionPolicy(TemplateV2VO template) {
        ExecutionPolicyVO policy = template.getExecutionPolicy();
        if (policy == null || StrKit.isBlank(policy.getMode())) {
            return;
        }
        String mode = policy.getMode().trim().toUpperCase(Locale.ROOT);
        if (!"IN_MEMORY".equals(mode) && !"CHUNKED".equals(mode) && !"STREAMING".equals(mode)) {
            throw new IllegalArgumentException("Unsupported execution policy mode: " + policy.getMode());
        }
        if (policy.getMaxRowsInMemory() != null && policy.getMaxRowsInMemory() <= 0) {
            throw new IllegalArgumentException("Execution policy maxRowsInMemory must be positive");
        }
        if (policy.getPreviewRowLimit() != null && policy.getPreviewRowLimit() <= 0) {
            throw new IllegalArgumentException("Execution policy previewRowLimit must be positive");
        }
        if (policy.getSourceChunkSize() != null && policy.getSourceChunkSize() <= 0) {
            throw new IllegalArgumentException("Execution policy sourceChunkSize must be positive");
        }
        if (policy.getSinkBatchSize() != null && policy.getSinkBatchSize() <= 0) {
            throw new IllegalArgumentException("Execution policy sinkBatchSize must be positive");
        }
        if (policy.getBroadcastMaxRows() != null && policy.getBroadcastMaxRows() <= 0) {
            throw new IllegalArgumentException("Execution policy broadcastMaxRows must be positive");
        }
        if (policy.getMaxTotalRows() != null && policy.getMaxTotalRows() <= 0) {
            throw new IllegalArgumentException("Execution policy maxTotalRows must be positive");
        }
        if (policy.getPartitionCount() != null && policy.getPartitionCount() <= 0) {
            throw new IllegalArgumentException("Execution policy partitionCount must be positive");
        }
        if ("CHUNKED".equals(mode)) {
            validateChunkedCompatibility(template);
        }
    }

    private static void validateChunkedCompatibility(TemplateV2VO template) {
        int broadcastMaxRows = EffectiveExecutionPolicy.resolve(template.getExecutionPolicy()).broadcastMaxRows();
        int unboundedQuerySources = ExecutionShapeClassifier.countUnboundedQuerySources(template, broadcastMaxRows);
        if (unboundedQuerySources >= 2) {
            throw new IllegalArgumentException(
                    "CHUNKED execution policy allows at most one unbounded QuerySourceVO, or a broadcast join "
                            + "with one dimension bounded by broadcastMaxRows ("
                            + broadcastMaxRows
                            + "); found "
                            + unboundedQuerySources
                            + " unbounded query sources");
        }
        ExecutionShape shape = ExecutionShapeClassifier.classify(template);
        // ROW_LOCAL and BROADCAST_JOIN are compatible with CHUNKED; only full materialization is rejected.
        if (shape == ExecutionShape.MATERIALIZATION_REQUIRED) {
            String features = describeMaterializationFeatures(firstSqlTransform(template));
            throw new IllegalArgumentException(
                    "CHUNKED execution policy is incompatible with SQL requiring materialization: " + features);
        }
    }

    private static String firstSqlTransform(TemplateV2VO template) {
        for (TransformVO transformer : template.getTransformers()) {
            if (transformer instanceof SqlTransformVO sqlTransform && StrKit.isNotBlank(sqlTransform.getSql())) {
                return sqlTransform.getSql();
            }
        }
        return "";
    }

    private static String describeMaterializationFeatures(String sql) {
        if (StrKit.isBlank(sql)) {
            return ExecutionShape.MATERIALIZATION_REQUIRED.name();
        }
        String normalized = sql.toUpperCase(Locale.ROOT);
        List<String> features = new ArrayList<>();
        if (normalized.contains("GROUP BY")) {
            features.add("GROUP BY");
        }
        if (SQL_JOIN.matcher(sql).find()) {
            features.add("JOIN");
        }
        if (SQL_SELECT_DISTINCT.matcher(sql).find()) {
            features.add("DISTINCT");
        }
        if (normalized.contains("ORDER BY")
                && !normalized.contains("FETCH")
                && !normalized.contains("LIMIT")) {
            features.add("ORDER BY without LIMIT");
        }
        if (features.isEmpty()) {
            return ExecutionShape.MATERIALIZATION_REQUIRED.name();
        }
        return String.join(", ", features);
    }

    /**
     * Kind of UDF reference detected in a Template V2 definition (D-10/D-27).
     */
    public enum UdfReferenceKind {
        /** SQL transform {@code sqlName} function-call token. */
        SQL,
        /** Script transform {@code udfRef:{id,version?}} block. */
        SCRIPT
    }

    /**
     * One UDF reference detected in a template, carrying enough context to resolve it against the registry
     * and to report the offending location when resolution fails (D-12).
     *
     * @param kind      reference form (SQL sqlName token or script udfRef block)
     * @param reference SQL {@code sqlName} token (SQL) or reverse-DNS {@code udfId} (script)
     * @param version   optional semver (script udfRef only; {@code null} → latest published)
     * @param path      template path of the owning transform, used as the error {@code field}
     */
    public record UdfReference(UdfReferenceKind kind, String reference, String version, String path) {
    }

    // Standard SQL / Calcite built-in function (and keyword-before-paren) names that are NOT registry UDFs.
    // Compared case-insensitively; any non-built-in function token is treated as a candidate sqlName (D-10/D-12).
    private static final Set<String> BUILT_IN_SQL_FUNCTIONS = Set.of(
            // structural clause keywords that can appear immediately before '(' (e.g. FROM (SELECT ...))
            "SELECT", "FROM", "WHERE", "AS", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS",
            "GROUP", "ORDER", "BY", "HAVING", "UNION", "INTERSECT", "EXCEPT", "WITH", "THEN", "ELSE",
            "END", "DISTINCT", "INTO", "LIMIT", "OFFSET", "FETCH", "ASC", "DESC",
            // logical / predicate keywords that can appear immediately before '('
            "IN", "EXISTS", "VALUES", "AND", "OR", "NOT", "BETWEEN", "LIKE", "ALL", "ANY", "SOME",
            "CASE", "WHEN", "ON", "USING", "OVER", "PARTITION",
            // aggregate / window built-ins
            "COUNT", "SUM", "AVG", "MIN", "MAX", "STDDEV", "VARIANCE", "ROW_NUMBER", "RANK", "DENSE_RANK",
            "LAG", "LEAD", "FIRST_VALUE", "LAST_VALUE",
            // scalar / string / math / date built-ins
            "COALESCE", "NULLIF", "GREATEST", "LEAST", "CAST", "CONVERT", "SUBSTRING", "SUBSTR", "TRIM",
            "LTRIM", "RTRIM", "UPPER", "LOWER", "CONCAT", "LENGTH", "CHAR_LENGTH", "REPLACE", "ABS",
            "ROUND", "FLOOR", "CEIL", "CEILING", "MOD", "POWER", "SQRT", "TRUNCATE", "IF", "IFNULL",
            "NVL", "EXTRACT", "NOW", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "DATE",
            "TIME", "TIMESTAMP", "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND", "ARRAY", "MAP");

    // Identifier immediately followed by '(' — a SQL function-call token candidate.
    private static final Pattern SQL_FUNCTION_CALL = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
    // Script udfRef:{ ... } block; the inner id/version are extracted separately (D-27).
    private static final Pattern SCRIPT_UDF_REF_BLOCK = Pattern.compile("udfRef\\s*:\\s*\\{([^}]*)}");
    private static final Pattern SCRIPT_UDF_REF_ID =
            Pattern.compile("\\bid\\s*:\\s*[\"']?([A-Za-z0-9_.]+)[\"']?");
    private static final Pattern SCRIPT_UDF_REF_VERSION =
            Pattern.compile("\\bversion\\s*:\\s*[\"']?(\\d+\\.\\d+\\.\\d+)[\"']?");

    /**
     * Collects the UDF references embedded in a normalized template (D-10/D-27): SQL {@code sqlName}
     * function-call tokens (excluding the built-in allow-list) and script {@code udfRef:{id,version?}}
     * blocks. Java/PF4J capabilities are intentionally not reference-validated (D-10). The same transform
     * traversal used by {@link #validate(TemplateV2VO)} is mirrored so linear transformers and compute-block
     * transforms (including transform-graph nodes) are all covered.
     *
     * @param template normalized template (may be {@code null})
     * @return detected references with their template paths; never {@code null}
     */
    public static List<UdfReference> collectUdfReferences(TemplateV2VO template) {
        List<UdfReference> references = new ArrayList<>();
        if (template == null) {
            return references;
        }
        if (!CollectKit.isEmpty(template.getTransformers())) {
            for (int index = 0; index < template.getTransformers().size(); index++) {
                collectFromTransform(template.getTransformers().get(index), "transformers[" + index + "]", references);
            }
        }
        if (!CollectKit.isEmpty(template.getComputeBlocks())) {
            for (int blockIndex = 0; blockIndex < template.getComputeBlocks().size(); blockIndex++) {
                ComputeBlockVO block = template.getComputeBlocks().get(blockIndex);
                if (block == null) {
                    continue;
                }
                String blockPath = "computeBlocks[" + blockIndex + "]";
                if (!CollectKit.isEmpty(block.getTransformers())) {
                    for (int index = 0; index < block.getTransformers().size(); index++) {
                        collectFromTransform(block.getTransformers().get(index),
                                blockPath + ".transformers[" + index + "]", references);
                    }
                }
                TransformGraphVO graph = block.getTransformGraph();
                if (graph != null && graph.getTransforms() != null) {
                    for (Map.Entry<String, TransformVO> entry : graph.getTransforms().entrySet()) {
                        collectFromTransform(entry.getValue(),
                                blockPath + ".transformGraph.transforms." + entry.getKey(), references);
                    }
                }
            }
        }
        return references;
    }

    private static void collectFromTransform(TransformVO transformer, String path, List<UdfReference> references) {
        if (transformer instanceof SqlTransformVO sqlTransform && StrKit.isNotBlank(sqlTransform.getSql())) {
            Matcher matcher = SQL_FUNCTION_CALL.matcher(sqlTransform.getSql());
            Set<String> seen = new HashSet<>();
            while (matcher.find()) {
                String token = matcher.group(1);
                // Built-ins are skipped; every remaining non-built-in token is a candidate sqlName (D-10/D-12).
                if (BUILT_IN_SQL_FUNCTIONS.contains(token.toUpperCase(Locale.ROOT)) || !seen.add(token)) {
                    continue;
                }
                references.add(new UdfReference(UdfReferenceKind.SQL, token, null, path + ".sql"));
            }
        }
        if (transformer instanceof JsTransformVO jsTransform && StrKit.isNotBlank(jsTransform.getScript())) {
            Matcher blockMatcher = SCRIPT_UDF_REF_BLOCK.matcher(jsTransform.getScript());
            while (blockMatcher.find()) {
                String block = blockMatcher.group(1);
                Matcher idMatcher = SCRIPT_UDF_REF_ID.matcher(block);
                if (!idMatcher.find()) {
                    continue;
                }
                Matcher versionMatcher = SCRIPT_UDF_REF_VERSION.matcher(block);
                String version = versionMatcher.find() ? versionMatcher.group(1) : null;
                references.add(new UdfReference(UdfReferenceKind.SCRIPT, idMatcher.group(1), version, path + ".script"));
            }
        }
    }
}
