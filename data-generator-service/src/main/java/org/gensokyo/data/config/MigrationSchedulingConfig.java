/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring scheduling for optional migration batch jobs.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(MigrationBatchCompareProperties.class)
public class MigrationSchedulingConfig {
}
