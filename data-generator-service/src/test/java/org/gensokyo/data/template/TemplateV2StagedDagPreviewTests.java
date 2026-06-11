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
 * DAG staged preview tests for {@link TemplateV2ControlPlaneService}.
 *
 * @author Gensokyo
 * @since 2026-06-10
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateV2StagedDagPreviewTests {

    @Autowired
    private TemplateV2ControlPlaneService service;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void previewThroughFirstDagNodeReturnsFewerColumnsThanFullGraph() {
        TemplatePO entity = new TemplatePO();
        entity.setId(97020L);
        entity.setName("staged-preview-dag");
        entity.setContentYaml("""
                name: staged-preview-dag
                workflow:
                  steps:
                    - type: invoke_compute_block
                      id: invoke-dag
                      computeBlockId: dag-block
                computeBlocks:
                  - id: dag-block
                    sources:
                      seed:
                        type: iterator
                        iterator:
                          type: number
                          from: 1
                          to: 5
                          step: 1
                    transformGraph:
                      transforms:
                        filter-high:
                          type: sql
                          name: filter-high
                          sql: SELECT value FROM seed WHERE value >= 4
                        shift-values:
                          type: sql
                          name: shift-values
                          sql: SELECT value, value + 10 AS shifted FROM input
                      nodes:
                        - id: n1
                          transformId: filter-high
                          outputAlias: filtered
                        - id: n2
                          transformId: shift-values
                          outputAlias: output
                      edges:
                        - fromNodeId: n1
                          fromPort: out
                          toNodeId: n2
                          toPort: in
                """);
        templateRepository.saveAndFlush(entity);

        TemplateV2PreviewDTO staged = service.preview(entity.getId(), 10, null, "dag-block", "n1");
        TemplateV2PreviewDTO full = service.preview(entity.getId(), 10);

        Assertions.assertNotNull(staged.getSchema());
        Assertions.assertNotNull(full.getSchema());
        Assertions.assertEquals(1, staged.getSchema().getColumns().size());
        Assertions.assertEquals(2, full.getSchema().getColumns().size());
        Assertions.assertTrue(staged.getSchema().contains("value"));
        Assertions.assertFalse(staged.getSchema().contains("shifted"));
        Assertions.assertTrue(full.getSchema().contains("shifted"));
    }
}
