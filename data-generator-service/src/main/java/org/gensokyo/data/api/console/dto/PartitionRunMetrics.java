/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Partition counters extracted from a V2 run metrics JSON payload.
 *
 * @param configuredPartitions configured partition count when partitioned
 * @param executedPartitions   partitions that processed rows
 * @author Gensokyo
 * @since 2026-06-01
 */
public record PartitionRunMetrics(int configuredPartitions, int executedPartitions) {
}
