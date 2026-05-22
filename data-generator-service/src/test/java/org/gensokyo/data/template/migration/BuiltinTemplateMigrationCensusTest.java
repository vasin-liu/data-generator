/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Invariants and report generation for {@link BuiltinTemplateMigrationCensus}.
 *
 * @author Gensokyo
 * @since 2026-05-22
 */
class BuiltinTemplateMigrationCensusTest {

    private static final Path REPORT_PATH =
            Path.of("..", "docs", "migration", "reports", "builtin-orchestration-census.md");

    @Test
    void censusCoversBuiltinCatalog() {
        BuiltinTemplateMigrationCensus.CensusResult result = BuiltinTemplateMigrationCensus.run();
        Assertions.assertTrue(result.summary().total() >= 50,
                () -> "expected ~61 builtins, got " + result.summary().total());
    }

    @Test
    void pauseRegressionFixtureIsCompatibilityOnly() throws Exception {
        String yaml = new ClassPathResource("migration/regression/v1-with-pause.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        TemplateVO v1 = new JacksonParser().parse(yaml, TemplateVO.class);
        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
        Assertions.assertEquals(MigrationClassification.COMPATIBILITY_ONLY, analysis.getSuggestedClass());
        Assertions.assertEquals("orchestration_legacy", analysis.getScenarioFamily());
        Assertions.assertEquals("compatibility_only", analysis.getRecommendedPath());
    }

    @Test
    void parking11IsSpelMigratable() {
        BuiltinTemplateMigrationCensus.Row row =
                findRow("tocc/parking/11_parking_online_space_record.yaml");
        Assertions.assertNotEquals(MigrationClassification.COMPATIBILITY_ONLY, row.analysis().getSuggestedClass());
        Assertions.assertEquals("spel", row.analysis().getRecommendedPath());
        // Database iterator without per-field JDBC readers is classified as synthetic by analyzer heuristics.
        Assertions.assertEquals("synthetic", row.analysis().getScenarioFamily());
    }

    @Test
    void compatibilityOnlyShareIsBelowMajority() {
        BuiltinTemplateMigrationCensus.Summary summary = BuiltinTemplateMigrationCensus.run().summary();
        int percent = Math.round(summary.compatibilityOnly() * 100f / summary.total());
        // Orchestration/JS blockers exist but most builtins are SpEL-migratable (2b cohort).
        Assertions.assertTrue(percent < 50,
                () -> "expected COMPATIBILITY_ONLY minority, got " + percent + "% (" + summary.compatibilityOnly()
                        + "/" + summary.total() + ")");
    }

    @Test
    void writesBuiltinOrchestrationCensusReport() throws Exception {
        BuiltinTemplateMigrationCensus.CensusResult result = BuiltinTemplateMigrationCensus.run();
        Files.writeString(REPORT_PATH, BuiltinTemplateMigrationCensus.toMarkdown(result));
        Assertions.assertTrue(Files.size(REPORT_PATH) > 500);
    }

    private static BuiltinTemplateMigrationCensus.Row findRow(String relativePath) {
        return BuiltinTemplateMigrationCensus.run().rows().stream()
                .filter(row -> relativePath.equals(row.relativePath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing fixture: " + relativePath));
    }
}
