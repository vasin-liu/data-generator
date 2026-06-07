/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.api.console.dto.ScenarioCatalogEntryDto;
import org.gensokyo.data.template.V2ScenarioCatalogService;
import org.gensokyo.data.template.editor.TemplateEditorPayload;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.gensokyo.data.template.editor.TemplateEditorRunSupport;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for scenario catalog endpoints on {@link ConsoleTemplateEditorController}.
 *
 * @author Gensokyo
 * @since 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
class ConsoleScenarioCatalogControllerTest {

    @Mock
    private TemplateEditorService templateEditorService;

    @Mock
    private TemplateEditorRunSupport templateEditorRunSupport;

    @Mock
    private V2ScenarioCatalogService scenarioCatalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConsoleTemplateEditorController(
                                templateEditorService, templateEditorRunSupport, scenarioCatalogService))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void listScenarios_returnsCatalogRows() throws Exception {
        when(scenarioCatalogService.listOfficial()).thenReturn(List.of(
                new ScenarioCatalogEntryDto("GF-A", "A", "scenario-a-synthetic", "SG-01", "scenario-a-synthetic.yaml")));

        mockMvc.perform(get("/api/templates/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].scenarioId").value("GF-A"))
                .andExpect(jsonPath("$.data[0].family").value("A"));
    }

    @Test
    void scaffoldFromScenario_returnsEditorPayload() throws Exception {
        when(templateEditorService.createDraftFromScenario("GF-JS"))
                .thenReturn(new TemplateEditorPayload(null, TemplateDefinitionKind.V2, null, null, false, "DRAFT"));

        mockMvc.perform(get("/api/templates/scenarios/GF-JS/scaffold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kind").value("V2"));
    }
}
