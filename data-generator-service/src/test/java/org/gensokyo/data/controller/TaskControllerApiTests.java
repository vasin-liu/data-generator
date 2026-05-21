/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.model.dto.TemplateDTO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@code /task} list, lookup, and run-by-name endpoints.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
@Import(TaskControllerApiTests.TaskApiTestConfig.class)
class TaskControllerApiTests {

    @Autowired
    private TaskController taskController;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TaskControllerV2ExecutionTests.CapturingTemplateV2Runner templateV2Runner;

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
        templateV2Runner.reset();
    }

    @Test
    void listReturnsAllPersistedTemplates() {
        saveV1Template(97001L, "task-list-a");
        saveV1Template(97002L, "task-list-b");

        R<List<TemplateDTO>> result = taskController.list();

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(2, result.getData().size());
    }

    @Test
    void findByIdReturnsTemplateWhenPresent() {
        TemplatePO entity = saveV1Template(97003L, "task-find-by-id");

        R<TemplateDTO> result = taskController.findById(entity.getId());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertNotNull(result.getData());
        Assertions.assertEquals("task-find-by-id", result.getData().getName());
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        R<TemplateDTO> result = taskController.findById(97999999L);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertNull(result.getData());
    }

    @Test
    void findByNameReturnsMatchingTemplates() {
        saveV1Template(97004L, "task-prefix-one");
        saveV1Template(97005L, "task-prefix-two");
        saveV1Template(97006L, "other-name");

        R<List<TemplateDTO>> result = taskController.findByName("task-prefix");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(2, result.getData().size());
    }

    @Test
    void runByNameFailsWhenTemplateMissing() {
        R<String> result = taskController.runByName("no-such-template");

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void runByNameFailsWhenMultipleTemplatesShareName() {
        saveV1Template(97007L, "duplicate-name");
        saveV1Template(97008L, "duplicate-name");

        R<String> result = taskController.runByName("duplicate-name");

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("Multiple templates"));
    }

    @Test
    void runByNameStartsV2Template() throws InterruptedException {
        TemplatePO entity = new TemplatePO();
        entity.setId(97009L);
        entity.setName("task-run-by-name-v2");
        entity.setContentJson("{}");
        entity.setContentYaml("""
                name: task-run-by-name-v2
                sources:
                  input:
                    type: iterator
                    iterator:
                      type: number
                      from: 1
                      to: 2
                transform:
                  type: sql
                  sql: SELECT value FROM input
                sink:
                  writers:
                    - type: console
                """);
        templateRepository.saveAndFlush(entity);

        R<String> result = taskController.runByName("task-run-by-name-v2");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("task-run-by-name-v2"));
        Assertions.assertTrue(templateV2Runner.awaitInvocation(5, TimeUnit.SECONDS));
        TemplateV2VO submitted = templateV2Runner.lastTemplate();
        Assertions.assertNotNull(submitted);
        Assertions.assertEquals(97009L, submitted.getId());
    }

    private TemplatePO saveV1Template(long id, String name) {
        TemplatePO entity = new TemplatePO();
        entity.setId(id);
        entity.setName(name);
        entity.setContentJson("{}");
        entity.setContentYaml("""
                name: %s
                iterator:
                  type: number
                  from: 1
                  to: 1
                output:
                  writers:
                    - type: console
                """.formatted(name));
        return templateRepository.saveAndFlush(entity);
    }

    @TestConfiguration
    static class TaskApiTestConfig {

        @Bean
        @Primary
        TaskControllerV2ExecutionTests.CapturingTemplateV2Runner capturingTemplateV2Runner() {
            return new TaskControllerV2ExecutionTests.CapturingTemplateV2Runner();
        }
    }
}
