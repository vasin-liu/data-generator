/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import org.gensokyo.data.api.console.dto.AuditEventView;
import org.gensokyo.data.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@link ConsoleAuditController}.
 *
 * @author Gensokyo
 * @since 2026-06-07
 */
@ExtendWith(MockitoExtension.class)
class ConsoleAuditControllerTest {

    @Mock
    private AuditService auditService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConsoleAuditController(auditService))
                .setControllerAdvice(new ConsoleApiAdvice())
                .build();
    }

    @Test
    void list_returnsAuditRows() throws Exception {
        Instant at = Instant.parse("2026-06-07T00:00:00Z");
        when(auditService.listRecent(null, null, 100))
                .thenReturn(List.of(
                        new AuditEventView(1L, at, "alice", "TEMPLATE_PUBLISH", "TEMPLATE", "42", Map.of("name", "demo"))));

        mockMvc.perform(get("/api/console/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].action").value("TEMPLATE_PUBLISH"))
                .andExpect(jsonPath("$.data[0].detail.name").value("demo"));
    }
}
