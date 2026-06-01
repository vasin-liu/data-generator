/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for scheduled batch migration dual-run compare.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Getter
@Setter
@ConfigurationProperties(prefix = MigrationBatchCompareProperties.PREFIX)
public class MigrationBatchCompareProperties {

    /** Property prefix for batch compare scheduling. */
    public static final String PREFIX = "data.generator.migration.batch-compare";

    /** When {@code true}, runs {@link org.gensokyo.data.template.migration.MigrationBatchCompareService} on a cron. */
    private boolean scheduledEnabled = false;

    /** Cron expression for nightly catalog dual-run (default 02:00 server time). */
    private String cron = "0 0 2 * * *";

    /** Passed to batch compare when triggered by the scheduler. */
    private boolean refreshInventoryFirst = true;

    private boolean skipCompatibilityOnly = true;

    private int maxTemplates = 50;
}
