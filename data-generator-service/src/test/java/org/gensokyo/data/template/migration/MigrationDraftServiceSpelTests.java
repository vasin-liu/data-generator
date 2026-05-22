/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.template.BuiltinClasspathTemplateCatalog;
import org.gensokyo.data.template.TemplateV2Normalizer;
import org.gensokyo.data.template.TemplateV2Validator;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

/**
 * Integration tests for SpEL attachment in {@link MigrationDraftService}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
class MigrationDraftServiceSpelTests {

    private final JacksonParser yamlParser = new JacksonParser();
    private final MigrationDraftService draftService = new MigrationDraftService();

    @Test
    void iteratorDraftIncludesSpelTransformForRegressionIteratorSimple() throws Exception {
        TemplateVO v1 = loadFixture("migration/regression/v1-iterator-simple.yaml");
        TemplateV2VO normalized = normalizeDraft(draftService.buildDraft(v1));

        Assertions.assertEquals(2, normalized.getTransformers().size());
        Assertions.assertInstanceOf(SqlTransformVO.class, normalized.getTransformers().get(0));
        Assertions.assertInstanceOf(SpelTransformVO.class, normalized.getTransformers().get(1));
        SpelTransformVO spel = (SpelTransformVO) normalized.getTransformers().get(1);
        Assertions.assertFalse(spel.getColumns().isEmpty());
        TemplateV2Validator.validate(normalized);
    }

    @Test
    void jdbcCompareDraftIncludesSpelTransformForParkingFixture() {
        BuiltinClasspathTemplateCatalog.Fixture fixture = BuiltinClasspathTemplateCatalog.loadAll().stream()
                .filter(f -> f.relativePath().contains("11_parking_online_space_record"))
                .findFirst()
                .orElseThrow();
        TemplateVO v1 = yamlParser.parse(fixture.yaml(), TemplateVO.class);
        TemplateV2VO normalized = normalizeDraft(draftService.buildDraftForCompare(v1));

        Assertions.assertTrue(normalized.getTransformers().stream().anyMatch(SpelTransformVO.class::isInstance));
        TemplateV2Validator.validate(normalized);
    }

    @Test
    void jdbcDraftIncludesSpelTransformForParkingFixture() {
        BuiltinClasspathTemplateCatalog.Fixture fixture = BuiltinClasspathTemplateCatalog.loadAll().stream()
                .filter(f -> f.relativePath().contains("11_parking_online_space_record"))
                .findFirst()
                .orElseThrow();
        TemplateVO v1 = yamlParser.parse(fixture.yaml(), TemplateVO.class);
        TemplateV2VO normalized = normalizeDraft(draftService.buildDraft(v1));

        Assertions.assertTrue(normalized.getTransformers().stream().anyMatch(SpelTransformVO.class::isInstance));
        TemplateV2Validator.validate(normalized);
    }

    @Test
    void syntheticCohortDemo28DraftIncludesSpelTransform() {
        BuiltinClasspathTemplateCatalog.Fixture fixture = BuiltinClasspathTemplateCatalog.loadAll().stream()
                .filter(f -> f.relativePath().contains("28_常量迭代器重复多次样例"))
                .findFirst()
                .orElseThrow();
        TemplateVO v1 = yamlParser.parse(fixture.yaml(), TemplateVO.class);
        TemplateV2VO normalized = normalizeDraft(draftService.buildDraft(v1));

        Assertions.assertTrue(normalized.getTransformers().stream().anyMatch(SpelTransformVO.class::isInstance));
        TemplateV2Validator.validate(normalized);
    }

    private TemplateV2VO normalizeDraft(TemplateV2DraftVO draft) {
        return TemplateV2Normalizer.normalize(draft);
    }

    private TemplateVO loadFixture(String classpath) throws Exception {
        String yaml = new ClassPathResource(classpath).getContentAsString(StandardCharsets.UTF_8);
        return yamlParser.parse(yaml, TemplateVO.class);
    }
}
