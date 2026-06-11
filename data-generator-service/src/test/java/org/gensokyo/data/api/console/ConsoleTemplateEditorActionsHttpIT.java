/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies Spring MVC deserializes console draft JSON with polymorphic {@code SourceVO} subtypes.
 *
 * @author Gensokyo
 * @since 2026-06-04
 */
@SpringBootTest(properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class ConsoleTemplateEditorActionsHttpIT {

    private static final String ITERATOR_DRAFT_JSON = """
            {
              "name": "console-http-iterator",
              "sources": {
                "seed": {
                  "type": "iterator",
                  "iterator": {
                    "type": "number",
                    "from": 1,
                    "to": 3,
                    "step": 1
                  }
                }
              },
              "transform": {
                "type": "sql",
                "sql": "SELECT value FROM seed"
              },
              "sink": {
                "writers": [
                  { "type": "console" }
                ]
              }
            }
            """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * Regression for {@code InvalidTypeIdException}: {@code type: iterator} on draft sources.
     */
    @Test
    void validateDraft_deserializesIteratorSourceSubtype() throws Exception {
        mockMvc.perform(post("/api/templates/draft/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ITERATOR_DRAFT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * Regression for {@link org.gensokyo.data.api.console.dto.DraftPreviewRequest} nested draft deserialization.
     */
    @Test
    void previewDraft_deserializesDraftPreviewRequestWithoutJsonParseError() throws Exception {
        String body = """
                {
                  "draft": %s,
                  "maxRows": 1
                }
                """.formatted(ITERATOR_DRAFT_JSON.trim());

        mockMvc.perform(post("/api/templates/draft/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertNotEquals(
                        500,
                        result.getResponse().getStatus(),
                        "JSON subtype registration should prevent HttpMessageNotReadableException"));
    }
}
