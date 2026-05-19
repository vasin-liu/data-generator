package org.gensokyo.data.template;

import org.gensokyo.data.calcite.sql.ExecutionShape;
import org.gensokyo.data.calcite.sql.ExecutionShapeClassifier;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

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
        if (CollectKit.isEmpty(template.getSources())) {
            throw new IllegalArgumentException("Template V2 sources must not be empty");
        }
        if (CollectKit.isEmpty(template.getTransformers())) {
            throw new IllegalArgumentException("Template V2 transformers must not be empty");
        }
        if (CollectKit.isEmpty(template.getSinks())) {
            throw new IllegalArgumentException("Template V2 sinks must not be empty");
        }

        var names = new HashSet<String>();
        boolean requireNames = template.getTransformers().size() > 1;
        for (var transformer : template.getTransformers()) {
            if (transformer == null) {
                throw new IllegalArgumentException("Template V2 transformer must not be null");
            }
            if (requireNames && StrKit.isBlank(transformer.getName())) {
                throw new IllegalArgumentException("Template V2 transformers must all be named when more than one transformer is configured");
            }
            if (StrKit.isNotBlank(transformer.getName()) && !names.add(transformer.getName())) {
                throw new IllegalArgumentException("Duplicate transformer name: " + transformer.getName());
            }
            if (transformer instanceof SqlTransformVO sqlTransform && StrKit.isBlank(sqlTransform.getSql())) {
                throw new IllegalArgumentException("SQL transformer SQL must not be blank");
            }
        }
        validateExecutionPolicy(template);

        for (var entry : template.getSources().entrySet()) {
            if (StrKit.isBlank(entry.getKey())) {
                throw new IllegalArgumentException("Template V2 source name must not be blank");
            }
            if (Objects.isNull(entry.getValue())) {
                throw new IllegalArgumentException("Template V2 source '" + entry.getKey() + "' must not be null");
            }
        }

        for (var sink : template.getSinks()) {
            if (sink == null || CollectKit.isEmpty(sink.getWriters())) {
                throw new IllegalArgumentException("Template V2 sink must contain at least one writer");
            }
        }

        validateSinkExecutionPolicy(template.getSinkExecutionPolicy());
    }

    private static void validateSinkExecutionPolicy(SinkExecutionPolicyVO policy) {
        if (policy == null || StrKit.isBlank(policy.getMode())) {
            return;
        }
        String mode = policy.getMode().trim().toUpperCase();
        if (!"FAIL_FAST".equals(mode) && !"CONTINUE_ON_ERROR".equals(mode)) {
            throw new IllegalArgumentException("Unsupported sink execution policy mode: " + policy.getMode());
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
        if ("STREAMING".equals(mode)) {
            throw new IllegalArgumentException("STREAMING execution policy mode is not implemented");
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
        if ("CHUNKED".equals(mode)) {
            validateChunkedCompatibility(template);
        }
    }

    private static void validateChunkedCompatibility(TemplateV2VO template) {
        ExecutionShape shape = ExecutionShapeClassifier.classify(template);
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
}
