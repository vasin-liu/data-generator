/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.stage.PauseStageVO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tests for {@link V1TemplateMigrationAnalyzer}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class V1TemplateMigrationAnalyzerTests {

    private final JacksonParser yamlParser = new JacksonParser();

    @Test
    void flagsCompatibilityOnlyWhenPauseStagePresent() throws Exception {
        TemplateVO v1 = loadFixture("migration/regression/v1-with-pause.yaml");
        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);

        Assertions.assertEquals(MigrationClassification.COMPATIBILITY_ONLY, analysis.getSuggestedClass());
        Assertions.assertEquals("compatibility_only", analysis.getRecommendedPath());
        Assertions.assertEquals("orchestration_legacy", analysis.getScenarioFamily());
        Assertions.assertTrue(analysis.getBlockers().stream().anyMatch(b -> b.toLowerCase().contains("pause")));
    }

    @Test
    void classifiesIteratorOnlySimpleAsWaveOneSql() throws Exception {
        TemplateVO v1 = loadFixture("migration/regression/v1-iterator-simple.yaml");
        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);

        Assertions.assertEquals(MigrationClassification.ADAPTED, analysis.getSuggestedClass());
        Assertions.assertEquals(1, analysis.getWave());
        Assertions.assertEquals("sql", analysis.getRecommendedPath());
        Assertions.assertEquals("synthetic", analysis.getScenarioFamily());
        Assertions.assertTrue(analysis.getBlockers().isEmpty());
    }

    @Test
    void classifiesJdbcLookupAsWaveTwoSql() throws Exception {
        TemplateVO v1 = loadFixture("migration/regression/v1-query-lookup.yaml");
        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);

        Assertions.assertEquals(MigrationClassification.ADAPTED, analysis.getSuggestedClass());
        Assertions.assertEquals(2, analysis.getWave());
        Assertions.assertEquals("sql", analysis.getRecommendedPath());
        Assertions.assertEquals("multi_source", analysis.getScenarioFamily());
    }

    @Test
    void flagsCompatibilityOnlyForJavaScriptScriptStage() {
        TemplateVO v1 = new TemplateVO();
        v1.setName("js-field");
        ScriptStageVO scriptStage = new ScriptStageVO();
        ScriptVO language = new ScriptVO();
        language.setType("JAVASCRIPT");
        language.setContent("return 1;");
        scriptStage.setLanguage(language);
        FieldVO field = new FieldVO();
        field.setName("js");
        field.setStages(List.of(scriptStage));
        v1.setFields(List.of(field));

        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);

        Assertions.assertEquals(MigrationClassification.COMPATIBILITY_ONLY, analysis.getSuggestedClass());
        Assertions.assertEquals("compatibility_only", analysis.getRecommendedPath());
        Assertions.assertTrue(analysis.getBlockers().stream().anyMatch(b -> b.toLowerCase().contains("javascript")));
    }

    @Test
    void flagsCompatibilityOnlyWhenPauseStageBuiltInMemory() {
        TemplateVO v1 = new TemplateVO();
        v1.setName("pause-inline");
        PauseStageVO pause = new PauseStageVO();
        pause.setDuration(1);
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setStages(List.of(pause));
        v1.setIterator(iterator);
        FieldVO field = new FieldVO();
        field.setName("x");
        field.setStages(List.of());
        v1.setFields(List.of(field));

        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);

        Assertions.assertEquals(MigrationClassification.COMPATIBILITY_ONLY, analysis.getSuggestedClass());
        Assertions.assertTrue(analysis.getBlockers().stream().anyMatch(b -> b.toLowerCase().contains("pause")));
    }

    private TemplateVO loadFixture(String classpath) throws Exception {
        String yaml = new ClassPathResource(classpath).getContentAsString(StandardCharsets.UTF_8);
        return yamlParser.parse(yaml, TemplateVO.class);
    }
}
