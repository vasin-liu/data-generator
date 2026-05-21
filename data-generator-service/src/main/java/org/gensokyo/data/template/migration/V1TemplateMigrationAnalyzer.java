/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.reader.JdbcReaderVO;
import org.gensokyo.data.stage.LogStageVO;
import org.gensokyo.data.stage.PauseStageVO;
import org.gensokyo.data.stage.SharedStageVO;
import org.gensokyo.data.template.querysource.V1QuerySourceMigrationWarningAnalyzer;
import org.gensokyo.kit.collect.CollectKit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Heuristic full-template migration analysis for V1 templates (scenario family, wave, blockers).
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class V1TemplateMigrationAnalyzer {

    private static final String PATH_SQL = "sql";
    private static final String PATH_SQL_UDF = "sql_udf";
    private static final String PATH_SPEL = "spel";
    private static final String PATH_COMPATIBILITY_ONLY = "compatibility_only";

    private V1TemplateMigrationAnalyzer() {
    }

    /**
     * Analyzes a V1 template for migration suitability.
     *
     * @param v1 V1 template definition (non-null)
     * @return analysis with suggested class, wave, path, blockers, and query-source warnings
     */
    public static TemplateMigrationAnalysisDTO analyze(TemplateVO v1) {
        Objects.requireNonNull(v1, "v1");
        List<String> warnings = new ArrayList<>(V1QuerySourceMigrationWarningAnalyzer.analyze(v1));
        LinkedHashSet<String> blockers = new LinkedHashSet<>();

        boolean javascript = false;
        boolean plainScript = false;
        boolean pause = false;
        boolean shared = false;
        boolean log = false;

        for (StageVO stage : collectStages(v1)) {
            if (stage == null) {
                continue;
            }
            if (isJavaScriptStage(stage)) {
                javascript = true;
            }
            if (isPlainScriptStage(stage)) {
                plainScript = true;
            }
            if (isPauseStage(stage)) {
                pause = true;
            }
            if (isSharedStage(stage)) {
                shared = true;
            }
            if (isLogStage(stage)) {
                log = true;
            }
        }

        if (javascript) {
            blockers.add("Template uses JavaScript script stages; retain on V1 or rewrite without GraalJS.");
        }
        if (pause) {
            blockers.add("Template uses PAUSE orchestration stage; V2 SQL migration cannot preserve pause semantics.");
        }
        if (shared) {
            blockers.add("Template uses SHARED orchestration stage; cross-row sharing is not modeled in V2 SQL drafts.");
        }
        if (log) {
            blockers.add("Template uses LOG orchestration stage; logging side effects are not migrated to V2 SQL.");
        }

        TemplateMigrationAnalysisDTO analysis = new TemplateMigrationAnalysisDTO();
        analysis.setWarnings(warnings);
        analysis.setScenarioFamily(inferScenarioFamily(v1, pause, shared, log));
        analysis.setBlockers(new ArrayList<>(blockers));

        if (javascript || pause || shared || log) {
            analysis.setSuggestedClass(MigrationClassification.COMPATIBILITY_ONLY);
            analysis.setRecommendedPath(PATH_COMPATIBILITY_ONLY);
            analysis.setWave(null);
            return analysis;
        }

        boolean hasJdbc = hasJdbcReader(v1);
        boolean readerPoolApproximation = hasReaderPoolApproximationWarning(warnings);
        boolean selectApproximation = hasSelectApproximationWarning(warnings);

        if (readerPoolApproximation || selectApproximation) {
            analysis.setSuggestedClass(MigrationClassification.APPROXIMATE);
        }
        else if (hasJdbc || isIteratorOnlySimple(v1)) {
            analysis.setSuggestedClass(MigrationClassification.ADAPTED);
        }
        else {
            analysis.setSuggestedClass(MigrationClassification.UNCLASSIFIED);
        }

        if (hasJdbc) {
            analysis.setWave(2);
            analysis.setRecommendedPath(selectApproximation ? PATH_SQL_UDF : PATH_SQL);
        }
        else if (isIteratorOnlySimple(v1)) {
            analysis.setWave(1);
            analysis.setRecommendedPath(PATH_SQL);
        }
        else {
            analysis.setWave(null);
            analysis.setRecommendedPath(readerPoolApproximation ? PATH_SQL_UDF : "custom");
        }

        // Plain SCRIPT fields map to V2 SpEL transform, not V1-only compatibility.
        if (plainScript) {
            analysis.setRecommendedPath(PATH_SPEL);
        }

        return analysis;
    }

    /**
     * Infers scenario family from template shape (aligned with inventory seeder heuristics).
     *
     * @param v1              template
     * @param orchestration   whether pause/shared/log stages were found
     * @return scenario family label
     */
    static String inferScenarioFamily(TemplateVO v1, boolean orchestration) {
        if (orchestration) {
            return "orchestration_legacy";
        }
        if (hasJdbcReader(v1)) {
            return "multi_source";
        }
        if (v1.getIterator() != null) {
            return "synthetic";
        }
        return "synthetic";
    }

    private static String inferScenarioFamily(TemplateVO v1, boolean pause, boolean shared, boolean log) {
        return inferScenarioFamily(v1, pause || shared || log);
    }

    private static boolean isIteratorOnlySimple(TemplateVO v1) {
        if (v1.getIterator() == null || CollectKit.isEmpty(v1.getFields())) {
            return false;
        }
        return !hasJdbcReader(v1);
    }

    private static boolean hasJdbcReader(TemplateVO v1) {
        if (CollectKit.isEmpty(v1.getFields())) {
            return false;
        }
        for (FieldVO field : v1.getFields()) {
            if (field == null || CollectKit.isEmpty(field.getStages())) {
                continue;
            }
            for (StageVO stage : field.getStages()) {
                if (!(stage instanceof ReadStageVO readStage) || CollectKit.isEmpty(readStage.getReaders())) {
                    continue;
                }
                for (ReaderVO reader : readStage.getReaders()) {
                    if (reader instanceof JdbcReaderVO) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasReaderPoolApproximationWarning(List<String> warnings) {
        for (String warning : warnings) {
            if (warning != null && warning.contains("reader-pool dispatch")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSelectApproximationWarning(List<String> warnings) {
        for (String warning : warnings) {
            if (warning == null) {
                continue;
            }
            if (warning.contains("ONCE_ORDER")
                    || warning.contains("ONCE_RANDOM")
                    || warning.contains("MULTIPLE_ORDER")
                    || warning.contains("SourcePolicyVO")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJavaScriptStage(StageVO stage) {
        if (!(stage instanceof ScriptStageVO scriptStage)) {
            return false;
        }
        ScriptVO language = scriptStage.getLanguage();
        if (language == null || language.getType() == null) {
            return false;
        }
        return "JAVASCRIPT".equalsIgnoreCase(language.getType().trim());
    }

    /**
     * Detects V1 SCRIPT stages using plain (SpEL-compatible) language, not GraalJS.
     *
     * @param stage stage from field or iterator pipeline
     * @return {@code true} when stage is SCRIPT with plain/default language
     */
    private static boolean isPlainScriptStage(StageVO stage) {
        if (!(stage instanceof ScriptStageVO scriptStage)) {
            return false;
        }
        ScriptVO language = scriptStage.getLanguage();
        if (language == null || language.getType() == null || language.getType().isBlank()) {
            return true;
        }
        String type = language.getType().trim();
        if ("JAVASCRIPT".equalsIgnoreCase(type)) {
            return false;
        }
        return "PLAIN".equalsIgnoreCase(type) || "plain".equalsIgnoreCase(type);
    }

    private static boolean isPauseStage(StageVO stage) {
        if (stage instanceof PauseStageVO) {
            return true;
        }
        return stageTypeEquals(stage, "PAUSE");
    }

    private static boolean isSharedStage(StageVO stage) {
        if (stage instanceof SharedStageVO) {
            return true;
        }
        return stageTypeEquals(stage, "SHARED");
    }

    private static boolean isLogStage(StageVO stage) {
        if (stage instanceof LogStageVO) {
            return true;
        }
        return stageTypeEquals(stage, "LOG");
    }

    private static boolean stageTypeEquals(StageVO stage, String expected) {
        if (expected == null || expected.isBlank() || stage == null || stage.getType() == null) {
            return false;
        }
        return expected.equalsIgnoreCase(stage.getType().trim());
    }

    private static List<StageVO> collectStages(TemplateVO v1) {
        List<StageVO> collected = new ArrayList<>();
        collectIteratorStages(v1.getIterator(), collected);
        if (CollectKit.isNotEmpty(v1.getFields())) {
            for (FieldVO field : v1.getFields()) {
                if (field != null && CollectKit.isNotEmpty(field.getStages())) {
                    collected.addAll(field.getStages());
                }
            }
        }
        return collected;
    }

    private static void collectIteratorStages(IteratorVO iterator, List<StageVO> collected) {
        if (iterator == null) {
            return;
        }
        if (CollectKit.isNotEmpty(iterator.getStages())) {
            collected.addAll(iterator.getStages());
        }
        collectIteratorStages(iterator.getIterator(), collected);
    }
}
