/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.v2.SpelColumnMapping;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.kit.collect.CollectKit;

import java.util.Locale;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds {@link SpelTransformVO} from V1 field {@code SCRIPT} stages for migration drafts.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class V1ScriptToSpelDraftConverter {

    private V1ScriptToSpelDraftConverter() {
    }

    /**
     * Returns whether the template has at least one migratable plain/SPEL script field.
     *
     * @param v1 V1 template
     * @return {@code true} when {@link #convert} would return a non-empty transform
     */
    public static boolean hasMigratableScriptFields(TemplateVO v1) {
        SpelTransformVO transform = convert(v1);
        return transform != null && CollectKit.isNotEmpty(transform.getColumns());
    }

    /**
     * Converts V1 SCRIPT fields into a SpEL transform, or {@code null} when none apply.
     *
     * @param v1 V1 template
     * @return SpEL transform with column mappings, or {@code null}
     * @throws IllegalArgumentException when {@code dependsOn} forms a cycle
     */
    public static SpelTransformVO convert(TemplateVO v1) {
        Objects.requireNonNull(v1, "v1");
        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
        if (analysis.getSuggestedClass() == MigrationClassification.COMPATIBILITY_ONLY) {
            return null;
        }
        if (CollectKit.isEmpty(v1.getFields())) {
            return null;
        }

        List<FieldVO> ordered = orderFieldsByDependsOn(v1.getFields());
        List<SpelColumnMapping> columns = new ArrayList<>();
        for (FieldVO field : ordered) {
            if (field == null || field.getName() == null || field.getName().isBlank()) {
                continue;
            }
            String expression = lastPlainScriptExpression(field);
            if (expression == null || expression.isBlank()) {
                continue;
            }
            SpelColumnMapping mapping = new SpelColumnMapping();
            mapping.setName(field.getName());
            mapping.setExpression(toV2SpelExpression(expression.trim(), field));
            columns.add(mapping);
        }
        if (columns.isEmpty()) {
            return null;
        }
        SpelTransformVO transform = new SpelTransformVO();
        transform.setColumns(columns);
        return transform;
    }

    private static String lastPlainScriptExpression(FieldVO field) {
        if (CollectKit.isEmpty(field.getStages())) {
            return null;
        }
        String last = null;
        for (StageVO stage : field.getStages()) {
            if (!V1TemplateMigrationAnalyzer.isPlainScriptStage(stage)) {
                continue;
            }
            ScriptStageVO scriptStage = (ScriptStageVO) stage;
            ScriptVO language = scriptStage.getLanguage();
            if (language == null || language.getContent() == null) {
                continue;
            }
            last = language.getContent();
        }
        return last;
    }

    private static List<FieldVO> orderFieldsByDependsOn(List<FieldVO> fields) {
        Map<String, FieldVO> byName = new LinkedHashMap<>();
        for (FieldVO field : fields) {
            if (field != null && field.getName() != null) {
                byName.put(field.getName(), field);
            }
        }
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> edges = new HashMap<>();
        for (String name : byName.keySet()) {
            indegree.put(name, 0);
            edges.put(name, new ArrayList<>());
        }
        for (FieldVO field : byName.values()) {
            List<String> deps = field.getDependsOn();
            if (CollectKit.isEmpty(deps)) {
                continue;
            }
            for (String dep : deps) {
                if (dep == null || !byName.containsKey(dep)) {
                    continue;
                }
                edges.get(dep).add(field.getName());
                indegree.merge(field.getName(), 1, Integer::sum);
            }
        }
        Deque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        List<FieldVO> ordered = new ArrayList<>(byName.size());
        while (!queue.isEmpty()) {
            String name = queue.removeFirst();
            ordered.add(byName.get(name));
            for (String next : edges.get(name)) {
                int remaining = indegree.merge(next, -1, Integer::sum);
                if (remaining == 0) {
                    queue.addLast(next);
                }
            }
        }
        if (ordered.size() != byName.size()) {
            throw new IllegalArgumentException("Field dependsOn graph contains a cycle");
        }
        return ordered;
    }

    private static String toV2SpelExpression(String expression, FieldVO field) {
        if (expression.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            // V1 bare identifiers such as "row" are string literals in field SCRIPT stages.
            return "'" + expression.replace("'", "''") + "'";
        }
        // V1 bare #dataset on dependsOn fields is the depended column value, not the SQL row — rewrite before #row mapping.
        String rewritten = expression;
        if (field != null && CollectKit.isNotEmpty(field.getDependsOn())) {
            String primaryDep = field.getDependsOn().getFirst();
            if (primaryDep != null && !primaryDep.isBlank()) {
                rewritten = rewritten.replaceAll(
                        "#dataset(?!\\.|\\[)\\b",
                        "#row['" + primaryDep.toLowerCase(Locale.ROOT) + "']");
            }
        }
        return V1SpelExpressionRewriter.rewrite(rewritten);
    }
}
