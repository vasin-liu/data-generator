/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.editor;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

/**
 * Verifies legacy V1 templates are rejected by {@link TemplateEditorService} load and save paths.
 *
 * @author Gensokyo &lt;liuweixing@pcitech.com&gt;
 * @since 2026-06-06
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplateEditorServiceV1RejectionTests {

    @Autowired
    private TemplateEditorService templateEditorService;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    /**
     * Ensures {@link TemplateEditorService#loadForEditor(Long)} refuses V1 rows.
     */
    @Test
    void loadForEditor_rejectsV1Template() throws Exception {
        TemplatePO v1 = persistV1Template();

        Assertions.assertEquals(TemplateDefinitionKind.V1, templateEditorService.detectDefinitionKind(v1));

        IllegalStateException thrown = Assertions.assertThrows(
                IllegalStateException.class,
                () -> templateEditorService.loadForEditor(v1.getId()));
        Assertions.assertTrue(thrown.getMessage().contains("V1"));
        Assertions.assertTrue(thrown.getMessage().contains("Legacy V1 templates are no longer supported"));
    }

    /**
     * Ensures {@link TemplateEditorService#save(Long, TemplateV2DraftVO)} refuses V1 rows before persist.
     */
    @Test
    void save_rejectsV1Template() throws Exception {
        TemplatePO v1 = persistV1Template();
        TemplateV2DraftVO draft = templateEditorService.createEmptyDraft().draft();
        draft.setName("attempted-v2-overwrite");

        IllegalStateException thrown = Assertions.assertThrows(
                IllegalStateException.class,
                () -> templateEditorService.save(v1.getId(), draft));
        Assertions.assertTrue(thrown.getMessage().contains("V1"));
        Assertions.assertTrue(thrown.getMessage().contains("Legacy V1 templates are no longer supported"));

        // Row must remain unchanged V1 YAML after rejected save.
        TemplatePO reloaded = templateRepository.findById(v1.getId()).orElseThrow();
        Assertions.assertEquals(TemplateDefinitionKind.V1, templateEditorService.detectDefinitionKind(reloaded));
        Assertions.assertEquals("legacy-v1", reloaded.getName());
    }

    private TemplatePO persistV1Template() throws Exception {
        String yaml = new ClassPathResource("migration/regression/v1-iterator-simple.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        TemplatePO entity = new TemplatePO();
        entity.setId(88001L);
        entity.setName("legacy-v1");
        entity.setContentYaml(yaml);
        entity.setArchived(false);
        return templateRepository.saveAndFlush(entity);
    }
}
