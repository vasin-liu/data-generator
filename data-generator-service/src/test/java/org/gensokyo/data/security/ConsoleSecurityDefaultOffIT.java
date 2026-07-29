/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.security;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.config.ConsoleSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regression guard that console RBAC stays disabled by default (Pitfall 4: accidental default-on).
 *
 * @author Gensokyo
 * @since 2026-07-29
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class ConsoleSecurityDefaultOffIT {

    @Autowired
    private ConsoleSecurityProperties consoleSecurityProperties;

    /**
     * Phase7 test profile must not enable RBAC unless explicitly overridden in test properties.
     */
    @Test
    void consoleSecurityDisabledByDefaultOnPhase7TestProfile() {
        assertFalse(consoleSecurityProperties.isEnabled(),
                "application-phase7-test.yaml must not enable console-security");
    }

    /**
     * Base {@code application.yaml} must not opt in to RBAC; Java field default {@code false} applies.
     */
    @Test
    void baseApplicationYamlDoesNotEnableConsoleSecurity() throws Exception {
        String yaml = new ClassPathResource("application.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        // Guard against profile bleed: no console-security block with enabled: true in base config.
        assertFalse(yaml.contains("console-security:") && yaml.contains("enabled: true"),
                "application.yaml must not set console-security.enabled: true");
    }
}
