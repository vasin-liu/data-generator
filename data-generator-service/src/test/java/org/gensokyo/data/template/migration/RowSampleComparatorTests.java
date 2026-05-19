/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for {@link RowSampleComparator}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
class RowSampleComparatorTests {

    @Test
    void returnsOneWhenSamplesMatchOnAllColumns() {
        List<Map<String, Object>> v1 = List.of(
                Map.of("id", 1, "name", "a"),
                Map.of("id", 2, "name", "b"));
        List<Map<String, Object>> v2 = List.of(
                Map.of("id", 1, "name", "a"),
                Map.of("id", 2, "name", "b"));

        double rate = RowSampleComparator.matchRate(v1, v2, null);
        Assertions.assertEquals(1.0d, rate, 1e-9);
    }

    @Test
    void returnsZeroWhenNoOverlappingRows() {
        double rate = RowSampleComparator.matchRate(List.of(Map.of("id", 1)), List.of(), null);
        Assertions.assertEquals(0.0d, rate, 1e-9);
    }

    @Test
    void comparesOnlyRequestedKeyColumns() {
        List<Map<String, Object>> v1 = List.of(Map.of("id", 1, "name", "a", "extra", "x"));
        List<Map<String, Object>> v2 = List.of(Map.of("id", 1, "name", "a", "extra", "y"));

        double allColumns = RowSampleComparator.matchRate(v1, v2, null);
        double idAndName = RowSampleComparator.matchRate(v1, v2, List.of("id", "name"));

        Assertions.assertEquals(0.0d, allColumns, 1e-9);
        Assertions.assertEquals(1.0d, idAndName, 1e-9);
    }

    @Test
    void returnsPartialMatchRateWhenSomeRowsDiffer() {
        List<Map<String, Object>> v1 = List.of(
                Map.of("id", 1),
                Map.of("id", 2));
        List<Map<String, Object>> v2 = List.of(
                Map.of("id", 1),
                Map.of("id", 3));

        double rate = RowSampleComparator.matchRate(v1, v2, List.of("id"));
        Assertions.assertEquals(0.5d, rate, 1e-9);
    }
}
