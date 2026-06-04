/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ConsoleUploadController}.
 *
 * @author Gensokyo
 * @since 2026-06-03
 */
class ConsoleUploadControllerTest {

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ConsoleUploadController())
                    .setControllerAdvice(new ConsoleApiAdvice())
                    .build();

    @Test
    void uploadFile_returnsAbsolutePath() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "sample.csv", "text/csv", "a,b\n1,2".getBytes());

        mockMvc.perform(multipart("/api/console/uploads/file").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void uploadInline_returnsAbsolutePath() throws Exception {
        mockMvc.perform(
                        post("/api/console/uploads/inline")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"{\\\"x\\\":1}\",\"filename\":\"inline.json\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void uploadInline_rejectsEmptyContent() throws Exception {
        mockMvc.perform(
                        post("/api/console/uploads/inline")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
