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
 * Verifies the React console bundle is copied into the service classpath at package time.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
@SpringBootTest
class ConsoleStaticResourceIT {

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * Ensures {@code static/console/index.html} exists after a full package build.
     */
    @Test
    void indexHtmlBundledInClasspath() {
        Resource index = resourceLoader.getResource("classpath:static/console/index.html");
        assertThat(index.exists()).isTrue();
    }
}
