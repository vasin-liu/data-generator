/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compares bounded row samples from V1 and V2 dual-run outputs.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class RowSampleComparator {

    private RowSampleComparator() {
    }

    /**
     * Compares two row samples positionally and returns the fraction of compared rows that match.
     *
     * @param v1Sample   V1 sample rows
     * @param v2Sample   V2 sample rows
     * @param keyColumns columns to compare; when {@code null}, uses key intersection per row
     * @return match rate in {@code [0, 1]}
     */
    public static double matchRate(
            List<Map<String, Object>> v1Sample,
            List<Map<String, Object>> v2Sample,
            List<String> keyColumns) {
        List<Map<String, Object>> left = v1Sample == null ? List.of() : v1Sample;
        List<Map<String, Object>> right = v2Sample == null ? List.of() : v2Sample;
        int pairs = Math.min(left.size(), right.size());
        if (pairs == 0) {
            return left.isEmpty() && right.isEmpty() ? 1.0d : 0.0d;
        }

        int matches = 0;
        for (int i = 0; i < pairs; i++) {
            List<String> columns = resolveColumns(left.get(i), right.get(i), keyColumns);
            if (rowsEqual(left.get(i), right.get(i), columns)) {
                matches++;
            }
        }
        return (double) matches / pairs;
    }

    private static List<String> resolveColumns(
            Map<String, Object> leftRow,
            Map<String, Object> rightRow,
            List<String> keyColumns) {
        if (keyColumns != null && !keyColumns.isEmpty()) {
            return keyColumns;
        }
        Set<String> intersection = new HashSet<>();
        if (leftRow != null) {
            intersection.addAll(leftRow.keySet());
        }
        if (rightRow != null) {
            intersection.retainAll(rightRow.keySet());
        }
        return new ArrayList<>(intersection);
    }

    private static boolean rowsEqual(Map<String, Object> leftRow, Map<String, Object> rightRow, List<String> columns) {
        if (columns.isEmpty()) {
            return Objects.equals(leftRow, rightRow);
        }
        for (String column : columns) {
            Object leftValue = leftRow == null ? null : leftRow.get(column);
            Object rightValue = rightRow == null ? null : rightRow.get(column);
            if (!Objects.equals(leftValue, rightValue)) {
                return false;
            }
        }
        return true;
    }
}
