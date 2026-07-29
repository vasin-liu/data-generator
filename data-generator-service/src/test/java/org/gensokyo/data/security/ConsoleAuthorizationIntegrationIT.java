/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.config.ConsoleSecurityProperties;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP integration tests for {@link ConsoleAuthorizationFilter} on the live Spring context.
 *
 * @author Gensokyo
 * @since 2026-06-10
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.console-security.enabled=true",
                "data.generator.governance.require-published-for-task-run=true"
        }
)
class ConsoleAuthorizationIntegrationIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ConsoleAuthorizationFilter consoleAuthorizationFilter;

    @Autowired
    private ConsoleSecurityProperties consoleSecurityProperties;

    @Autowired
    private DataGeneratorProperties dataGeneratorProperties;

    @Autowired
    private TemplateRepository templateRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void enableRbacForTest() {
        consoleSecurityProperties.setEnabled(true);
        dataGeneratorProperties.getGovernance().setRequirePublishedForTaskRun(true);
        // webAppContextSetup does not always apply @Component servlet filters in this test slice.
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(consoleAuthorizationFilter)
                .build();
    }

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void missingRoleHeaderReturnsForbiddenOnConsoleApi() throws Exception {
        mockMvc.perform(get("/api/templates/scenarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCanGetScenariosCatalogWhenSecurityEnabled() throws Exception {
        mockMvc.perform(get("/api/templates/scenarios")
                        .header("X-Console-Role", "VIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void viewerCannotCreateTemplateWhenSecurityEnabled() throws Exception {
        mockMvc.perform(post("/api/templates")
                        .header("X-Console-Role", "VIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"rbac-it\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void editorCannotPublishWhenSecurityEnabled() throws Exception {
        TemplatePO entity = new TemplatePO();
        entity.setId(99101L);
        entity.setName("rbac-editor-publish");
        entity.setStatus("DRAFT");
        entity.setContentYaml(loadScenarioYaml());
        templateRepository.saveAndFlush(entity);

        mockMvc.perform(post("/api/templates/" + entity.getId() + "/publish")
                        .header("X-Console-Role", "EDITOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotRunDraftTemplateFromCatalogApi() throws Exception {
        TemplatePO entity = new TemplatePO();
        entity.setId(99100L);
        entity.setName("rbac-draft-run");
        entity.setStatus("DRAFT");
        entity.setContentYaml(loadScenarioYaml());
        templateRepository.saveAndFlush(entity);

        mockMvc.perform(post("/api/templates/" + entity.getId() + "/run")
                        .header("X-Console-Role", "OPERATOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("PUBLISHED")));
    }

    private static String loadScenarioYaml() throws Exception {
        return new ClassPathResource("template/v2-scenarios/scenario-a-synthetic.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
