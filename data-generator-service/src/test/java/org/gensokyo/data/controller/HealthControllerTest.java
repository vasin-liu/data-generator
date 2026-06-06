/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link HealthController}.
 *
 * @author Gensokyo
 * @since 2026-06-06
 */
class HealthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HealthController controller = new HealthController();
        ReflectionTestUtils.setField(controller, "applicationName", "data-generator-service");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void healthzReturnsUpWithLegacyOpcode() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opcode").value(0))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("data-generator-service"));
    }
}
