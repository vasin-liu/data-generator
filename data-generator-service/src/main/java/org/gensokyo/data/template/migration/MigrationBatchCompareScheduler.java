/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.config.MigrationBatchCompareProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Optional nightly batch dual-run over the migration inventory catalog.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = MigrationBatchCompareProperties.PREFIX,
        name = "scheduled-enabled",
        havingValue = "true")
public class MigrationBatchCompareScheduler {

    private final MigrationBatchCompareService batchCompareService;
    private final MigrationBatchCompareProperties properties;

    /**
     * Creates the scheduler.
     *
     * @param batchCompareService batch compare service
     * @param properties          scheduling properties
     */
    public MigrationBatchCompareScheduler(
            MigrationBatchCompareService batchCompareService,
            MigrationBatchCompareProperties properties) {
        this.batchCompareService = batchCompareService;
        this.properties = properties;
    }

    /**
     * Runs catalog dual-run compare on the configured cron.
     */
    @Scheduled(cron = "${pci.data.generator.migration.batch-compare.cron:0 0 2 * * *}")
    public void runScheduledBatchCompare() {
        MigrationBatchCompareOptions options = new MigrationBatchCompareOptions();
        options.setRefreshInventoryFirst(properties.isRefreshInventoryFirst());
        options.setSkipCompatibilityOnly(properties.isSkipCompatibilityOnly());
        options.setMaxTemplates(properties.getMaxTemplates());
        options.setCompareOptions(MigrationCompareOptions.defaults());

        log.info("Starting scheduled migration batch compare (maxTemplates={})", properties.getMaxTemplates());
        MigrationBatchCompareResult result = batchCompareService.runBatch(options);
        log.info(
                "Scheduled migration batch compare finished: success={}, skipped={}, failed={}",
                result.getComparedCount(),
                result.getSkippedCount(),
                result.getFailedCount());
    }
}
