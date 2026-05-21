/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.migration.TemplateMigrationAnalysisDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {@code GET /template/migration/analyze/{templateId}}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateControllerMigrationAnalyzeTests {

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void analyzeMigrationReturnsScenarioFamilyForIteratorTemplate() {
        TemplatePO entity = new TemplatePO();
        entity.setId(95001L);
        entity.setName("analyze-iterator");
        entity.setContentYaml("""
                name: analyze-iterator
                iterator:
                  type: number
                  from: 1
                  to: 3
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateMigrationAnalysisDTO> result = templateController.analyzeMigration(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        TemplateMigrationAnalysisDTO analysis = result.getData();
        Assertions.assertNotNull(analysis);
        Assertions.assertNotNull(analysis.getScenarioFamily());
        Assertions.assertNotNull(analysis.getRecommendedPath());
    }

    @Test
    void analyzeMigrationFailsWhenTemplateMissing() {
        R<TemplateMigrationAnalysisDTO> result = templateController.analyzeMigration(99999999L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("does not exist"));
    }
}
