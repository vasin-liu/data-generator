/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Staged preview tests for {@link TemplateV2ControlPlaneService}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateV2StagedPreviewTests {

    @Autowired
    private TemplateV2ControlPlaneService service;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void previewThroughFirstTransformReturnsFewerColumnsThanFullChain() {
        TemplatePO entity = new TemplatePO();
        entity.setId(97010L);
        entity.setName("staged-preview-multi-transform");
        entity.setContentYaml("""
                name: staged-preview-multi-transform
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 3
                      step: 1
                transformers:
                  - name: pick_value
                    type: sql
                    sql: SELECT value FROM input
                  - name: add_shifted
                    type: sql
                    sql: SELECT value, value + 10 AS shifted FROM input
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        TemplateV2PreviewDTO staged = service.preview(entity.getId(), 10, 0);
        TemplateV2PreviewDTO full = service.preview(entity.getId(), 10);

        Assertions.assertNotNull(staged.getSchema());
        Assertions.assertNotNull(full.getSchema());
        Assertions.assertEquals(1, staged.getSchema().getColumns().size());
        Assertions.assertEquals(2, full.getSchema().getColumns().size());
        Assertions.assertTrue(staged.getSchema().contains("value"));
        Assertions.assertFalse(staged.getSchema().contains("shifted"));
        Assertions.assertTrue(full.getSchema().contains("shifted"));
    }

    @Test
    void previewRejectsOutOfRangeThroughTransformIndex() {
        TemplatePO entity = new TemplatePO();
        entity.setId(97011L);
        entity.setName("staged-preview-index-bounds");
        entity.setContentYaml("""
                name: staged-preview-index-bounds
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 2
                      step: 1
                transformers:
                  - name: pick_value
                    type: sql
                    sql: SELECT value FROM input
                  - name: add_shifted
                    type: sql
                    sql: SELECT value, value + 10 AS shifted FROM input
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.preview(entity.getId(), 10, 2));

        Assertions.assertTrue(ex.getMessage().contains("throughTransformIndex"));
    }
}
