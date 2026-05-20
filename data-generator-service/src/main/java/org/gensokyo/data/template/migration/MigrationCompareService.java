/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.template.querysource.V1QuerySourceMigrationWarningAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates dual-run compare of V1 and V2 templates and classifies the outcome.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class MigrationCompareService {

    private final TemplateRunExecutor executor;
    private final MigrationPlanExplainService planExplainService;

    /**
     * Creates the service with the given run executor (production pipeline or test stub).
     *
     * @param executor template run executor
     */
    public MigrationCompareService(TemplateRunExecutor executor) {
        this(executor, new MigrationPlanExplainService());
    }

    /**
     * Creates the service with executor and plan explain helper (tests may inject a stub).
     *
     * @param executor            template run executor
     * @param planExplainService    plan summary builder
     */
    public MigrationCompareService(TemplateRunExecutor executor, MigrationPlanExplainService planExplainService) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.planExplainService = Objects.requireNonNull(planExplainService, "planExplainService");
    }

    /**
     * Compares V1 and V2 template definitions.
     *
     * @param v1   V1 template
     * @param v2   V2 template (normalized)
     * @param opts compare options; when {@code null}, defaults are used
     * @return comparison report with classification and recommendation
     */
    public MigrationComparisonReport compare(TemplateVO v1, TemplateV2VO v2, MigrationCompareOptions opts) {
        Objects.requireNonNull(v1, "v1");
        Objects.requireNonNull(v2, "v2");
        MigrationCompareOptions options = opts != null ? opts : MigrationCompareOptions.defaults();

        List<String> warnings = collectWarnings(v1);
        Map<String, Object> params = Collections.emptyMap();

        RunOutcome v1Outcome = executor.runV1(v1, params, options);
        RunOutcome v2Outcome = executor.runV2(v2, params, options);

        List<Map<String, Object>> v1Sample = boundedSample(v1Outcome.sample(), options.getSampleSize());
        List<Map<String, Object>> v2Sample = boundedSample(v2Outcome.sample(), options.getSampleSize());

        double matchRate = RowSampleComparator.matchRate(v1Sample, v2Sample, options.getKeyColumns());
        MigrationClassification classification = MigrationClassificationRules.classify(
                v1Outcome.rowCount(),
                v2Outcome.rowCount(),
                matchRate,
                warnings);

        MigrationComparisonReport report = new MigrationComparisonReport();
        report.setTemplateId(v1.getId());
        report.setV1RowCount(v1Outcome.rowCount());
        report.setV2RowCount(v2Outcome.rowCount());
        report.setSampleSize(Math.min(v1Sample.size(), v2Sample.size()));
        report.setSampleMatchRate(matchRate);
        report.setClassification(classification);
        report.setWarnings(warnings);
        report.applyRecommendationFromClassification();
        report.setPlanExplain(planExplainService.explain(v1, v2));
        return report;
    }

    /**
     * Compares templates and sets {@link MigrationComparisonReport#getTemplateId()} from the argument.
     *
     * @param templateId persisted template id
     * @param v1         V1 template
     * @param v2         V2 template
     * @param opts       compare options
     * @return comparison report
     */
    public MigrationComparisonReport compare(
            Long templateId,
            TemplateVO v1,
            TemplateV2VO v2,
            MigrationCompareOptions opts) {
        MigrationComparisonReport report = compare(v1, v2, opts);
        report.setTemplateId(templateId);
        return report;
    }

    private static List<String> collectWarnings(TemplateVO v1) {
        List<String> warnings = new ArrayList<>();
        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
        if (analysis.getWarnings() != null) {
            warnings.addAll(analysis.getWarnings());
        }
        if (analysis.getBlockers() != null) {
            warnings.addAll(analysis.getBlockers());
        }
        warnings.addAll(V1QuerySourceMigrationWarningAnalyzer.analyze(v1));
        return warnings;
    }

    private static List<Map<String, Object>> boundedSample(
            List<Map<String, Object>> rows,
            int sampleSize) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        int limit = sampleSize > 0 ? sampleSize : 500;
        if (rows.size() <= limit) {
            return List.copyOf(rows);
        }
        return List.copyOf(rows.subList(0, limit));
    }
}
