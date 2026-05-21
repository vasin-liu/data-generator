/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.template.migration.MigrationClassification;
import org.gensokyo.data.template.migration.MigrationDraftService;
import org.gensokyo.data.template.migration.TemplateMigrationAnalysisDTO;
import org.gensokyo.data.template.migration.V1IteratorDraftConverter;
import org.gensokyo.data.template.migration.V1TemplateMigrationAnalyzer;
import org.gensokyo.data.template.querysource.V1QuerySourceExtractor;
import org.gensokyo.data.yaml.JacksonParser;
import org.gensokyo.kit.collect.CollectKit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Regression over all built-in classpath templates: parse, migration analyze, and draft build.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class BuiltinClasspathTemplateRegressionTests {

    private final JacksonParser yamlParser = new JacksonParser();
    private final TemplateV1Loader v1Loader = new TemplateV1Loader(yamlParser);
    private final MigrationDraftService draftService = new MigrationDraftService();

    @Test
    void allBuiltinTemplatesParseAsV1Yaml() {
        List<String> failures = new ArrayList<>();
        List<BuiltinClasspathTemplateCatalog.Fixture> fixtures = BuiltinClasspathTemplateCatalog.loadAll();

        Assertions.assertTrue(fixtures.size() >= 50, () -> "expected ~61 built-in templates, got " + fixtures.size());

        for (BuiltinClasspathTemplateCatalog.Fixture fixture : fixtures) {
            try {
                TemplateVO template = yamlParser.parse(fixture.yaml(), TemplateVO.class);
                Assertions.assertNotNull(template, fixture.displayName());
                Assertions.assertNotNull(template.getName(), fixture.displayName() + " name");
            }
            catch (Exception e) {
                failures.add(fixture.displayName() + ": parse " + e.getMessage());
            }
        }
        Assertions.assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void allBuiltinTemplatesAnalyzeWithoutError() {
        List<String> failures = new ArrayList<>();

        for (BuiltinClasspathTemplateCatalog.Fixture fixture : BuiltinClasspathTemplateCatalog.loadAll()) {
            try {
                TemplateVO v1 = loadV1(fixture);
                TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
                Assertions.assertNotNull(analysis.getSuggestedClass(), fixture.displayName());
                Assertions.assertNotNull(analysis.getRecommendedPath(), fixture.displayName());
                Assertions.assertNotNull(analysis.getScenarioFamily(), fixture.displayName());
            }
            catch (Exception e) {
                failures.add(fixture.displayName() + ": analyze " + e.getMessage());
            }
        }
        Assertions.assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void migratableBuiltinTemplatesBuildNormalizedDraft() {
        List<String> failures = new ArrayList<>();
        int exercised = 0;

        for (BuiltinClasspathTemplateCatalog.Fixture fixture : BuiltinClasspathTemplateCatalog.loadAll()) {
            try {
                TemplateVO v1 = loadV1(fixture);
                TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
                if (analysis.getSuggestedClass() == MigrationClassification.COMPATIBILITY_ONLY) {
                    continue;
                }
                boolean iteratorPath = V1IteratorDraftConverter.supports(v1);
                boolean queryPath = CollectKit.isNotEmpty(V1QuerySourceExtractor.extract(v1));
                if (!iteratorPath && !queryPath) {
                    continue;
                }
                TemplateV2DraftVO draft = draftService.buildDraft(v1);
                TemplateV2VO normalized = TemplateV2Normalizer.normalize(draft);
                if (CollectKit.isEmpty(normalized.getTransformers())) {
                    // Field-only JDBC templates may build a partial draft without transform yet.
                    continue;
                }
                exercised++;
                TemplateV2Validator.validate(normalized);
            }
            catch (Exception e) {
                failures.add(fixture.displayName() + ": draft " + e.getMessage());
            }
        }
        final int migratableCount = exercised;
        Assertions.assertTrue(migratableCount >= 20, () -> "expected many migratable built-in templates, got " + migratableCount);
        Assertions.assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    private TemplateVO loadV1(BuiltinClasspathTemplateCatalog.Fixture fixture) {
        TemplatePO entity = new TemplatePO();
        entity.setId(fixture.stableTemplateId());
        entity.setName(fixture.inventoryId());
        entity.setContentYaml(fixture.yaml());
        return v1Loader.load(entity);
    }
}
