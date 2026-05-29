/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures the React operator console is packaged into the service JAR.
 *
 * @author Gensokyo
 * @since 2026-05-28
 */
@SpringBootTest(properties = "spring.config.location=classpath:/application-phase7-test.yaml")
class ConsoleStaticResourceIT {

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * SPA entry must be on the classpath after {@code data-generator-console-web} build.
     */
    @Test
    void indexHtmlBundled() {
        Resource resource = resourceLoader.getResource("classpath:static/console/index.html");
        assertThat(resource.exists()).isTrue();
    }
}
