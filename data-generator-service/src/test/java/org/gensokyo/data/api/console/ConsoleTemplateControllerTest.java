/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.template.editor.TemplateEditorService;
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
 * Standalone tests for {@link ConsoleTemplateController}.
 *
 * @author Gensokyo
 * @since 2026-05-28
 */
@ExtendWith(MockitoExtension.class)
class ConsoleTemplateControllerTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateEditorService templateEditorService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConsoleTemplateController(templateRepository, templateEditorService))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void list_returnsV2SummariesOnly() throws Exception {
        TemplatePO v2 = new TemplatePO();
        v2.setId(42L);
        v2.setName("v2-demo");
        v2.setArchived(Boolean.FALSE);
        TemplatePO v1 = new TemplatePO();
        v1.setId(99L);
        v1.setName("legacy-v1");
        v1.setArchived(Boolean.FALSE);
        when(templateRepository.findByArchivedFalse()).thenReturn(List.of(v2, v1));
        when(templateEditorService.detectDefinitionKind(v2)).thenReturn(TemplateDefinitionKind.V2);
        when(templateEditorService.detectDefinitionKind(v1)).thenReturn(TemplateDefinitionKind.V1);

        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("v2-demo"));
    }
}
