/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.migration;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.template.migration.MigrationInventorySummary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Smoke tests for {@link MigrationConsoleService} inventory reads.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class MigrationConsoleServiceTests {

    @Autowired
    private MigrationConsoleService migrationConsoleService;

    @Test
    void summaryAndBacklogLoad() {
        MigrationInventorySummary summary = migrationConsoleService.summary();
        Assertions.assertNotNull(summary);
        Assertions.assertTrue(summary.getTotalTemplates() >= 0);
        Assertions.assertNotNull(migrationConsoleService.backlog("ALL"));
        Assertions.assertNotNull(migrationConsoleService.backlog("COMPATIBILITY_ONLY"));
    }
}
