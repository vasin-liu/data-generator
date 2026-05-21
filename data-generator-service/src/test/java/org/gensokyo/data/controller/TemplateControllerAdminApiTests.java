/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.cache.Templates;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.qo.UpdateTemplateQO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.YamlParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Integration tests for template admin endpoints (update, reload, refresh, upload).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TemplateControllerAdminApiTests.AdminTestConfig.class)
class TemplateControllerAdminApiTests {

    @Autowired
    private TemplateController templateController;

    @Autowired
    private TemplateRepository templateRepository;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void updateByIdPersistsYamlContent() {
        TemplatePO entity = new TemplatePO();
        entity.setId(96001L);
        entity.setName("admin-update-before");
        entity.setContentYaml("""
                name: admin-update-before
                iterator:
                  type: number
                  from: 1
                  to: 1
                output:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        UpdateTemplateQO qo = new UpdateTemplateQO();
        qo.setId(entity.getId());
        qo.setYaml("""
                name: admin-update-after
                iterator:
                  type: number
                  from: 1
                  to: 2
                output:
                  writers:
                    - type: console
                """);

        R<String> result = templateController.updateById(qo);

        Assertions.assertTrue(result.isSuccess());
        TemplatePO persisted = templateRepository.findById(entity.getId()).orElseThrow();
        Assertions.assertEquals("admin-update-after", persisted.getName());
        Assertions.assertTrue(persisted.getContentYaml().contains("to: 2"));
    }

    @Test
    void updateByIdFailsWhenTemplateMissing() {
        UpdateTemplateQO qo = new UpdateTemplateQO();
        qo.setId(96099L);
        qo.setYaml("""
                name: missing
                iterator:
                  type: number
                  from: 1
                  to: 1
                output:
                  writers:
                    - type: console
                """);

        R<String> result = templateController.updateById(qo);

        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    void reloadFromFileUsesTemplatesCache() {
        R<String> result = templateController.reloadFromFile();

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("0"));
    }

    @Test
    void refreshV2RuntimeReturnsOk() {
        R<String> result = templateController.refreshV2Runtime();

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("refreshed"));
    }

    @Test
    void uploadTemplatePersistsParsedYaml() {
        String yaml = """
                name: admin-uploaded
                iterator:
                  type: number
                  from: 1
                  to: 1
                output:
                  writers:
                    - type: console
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "admin-uploaded.yaml",
                "text/yaml",
                yaml.getBytes(StandardCharsets.UTF_8));

        R<String> result = templateController.uploadTemplate(file);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(templateRepository.findAll().stream()
                .anyMatch(t -> "admin-uploaded".equals(t.getName())));
    }

    @TestConfiguration
    static class AdminTestConfig {

        @Bean
        @Primary
        Templates emptyReloadTemplates(
                DataGeneratorProperties properties,
                YamlParser yamlParser,
                TemplateRepository repository) {
            return new Templates(properties, yamlParser, repository) {
                @Override
                public List<TemplatePO> reloadAll() {
                    return List.of();
                }
            };
        }
    }
}
