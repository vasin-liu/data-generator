/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.model.v2.Row;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a row stream into fixed-size buckets for in-process partitioned compute.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
final class RowPartitioner {

    private RowPartitioner() {
    }

    /**
     * Assigns rows to {@code partitionCount} buckets using hash partitioning or round-robin.
     *
     * @param rows           source rows to split
     * @param partitionCount target bucket count
     * @param partitionKey   optional column name for hash partitioning; round-robin when blank
     * @return one list per partition index
     */
    static List<List<Row>> partition(List<Row> rows, int partitionCount, String partitionKey) {
        if (partitionCount <= 0) {
            throw new IllegalArgumentException("partitionCount must be positive");
        }
        @SuppressWarnings("unchecked")
        List<Row>[] buckets = new List[partitionCount];
        for (int index = 0; index < partitionCount; index++) {
            buckets[index] = new ArrayList<>();
        }
        if (partitionKey != null && !partitionKey.isBlank()) {
            for (Row row : rows) {
                int bucket = Math.floorMod(hashKey(row.get(partitionKey)), partitionCount);
                buckets[bucket].add(row);
            }
        } else {
            for (int index = 0; index < rows.size(); index++) {
                buckets[index % partitionCount].add(rows.get(index));
            }
        }
        return List.of(buckets);
    }

    private static int hashKey(Object key) {
        return key == null ? 0 : key.hashCode();
    }
}
