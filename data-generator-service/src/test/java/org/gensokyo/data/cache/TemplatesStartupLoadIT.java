/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.cache;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies classpath templates are loaded into the repository on application startup.
 *
 * @author Gensokyo
 * @since 2026-05-28
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class TemplatesStartupLoadIT {

    @Autowired
    private TemplateRepository templateRepository;

    /**
     * Demo templates under {@code classpath:/template/demo} must be indexed at startup.
     */
    @Test
    void startupLoadsClasspathTemplates() {
        long count = templateRepository.count();
        assertTrue(count > 0, "expected classpath templates in repository, found " + count);
    }
}
