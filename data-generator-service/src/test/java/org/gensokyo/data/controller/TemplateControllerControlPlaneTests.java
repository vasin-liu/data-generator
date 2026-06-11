/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.qo.ValidateTemplateQO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateV2ValidationResult;
import org.gensokyo.data.template.TemplateV2PlanExplain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for Template V2 control-plane REST endpoints.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateControllerControlPlaneTests {

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void validateV2ReturnsOkForValidDraftYaml() {
        ValidateTemplateQO qo = new ValidateTemplateQO();
        qo.setYaml("""
                name: control-plane-validate
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: constant
                      count: 2
                transform:
                  type: sql
                  sql: SELECT 1 AS value FROM input
                sink:
                  writers:
                    - type: console
                """);

        R<TemplateV2ValidationResult> result = templateController.validateV2(qo);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertNotNull(result.getData());
        Assertions.assertTrue(result.getData().isValid());
    }

    @Test
    void explainV2ReturnsOkForSeededIteratorTemplate() {
        TemplatePO entity = new TemplatePO();
        entity.setId(98001L);
        entity.setName("control-plane-explain-rest");
        entity.setContentYaml("""
                name: control-plane-explain-rest
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 3
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<TemplateV2PlanExplain> result = templateController.explainV2(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        TemplateV2PlanExplain explain = result.getData();
        Assertions.assertNotNull(explain);
        Assertions.assertFalse(explain.getSourceSummaries().isEmpty());
    }
}
