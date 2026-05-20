/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
        return caseInsensitiveColumnIntersection(leftRow, rightRow);
    }

    private static List<String> caseInsensitiveColumnIntersection(
            Map<String, Object> leftRow,
            Map<String, Object> rightRow) {
        if (leftRow == null || rightRow == null) {
            return List.of();
        }
        Set<String> columns = new LinkedHashSet<>();
        for (String leftKey : leftRow.keySet()) {
            if (leftKey == null) {
                continue;
            }
            for (String rightKey : rightRow.keySet()) {
                if (columnNamesMatch(leftKey, rightKey)) {
                    columns.add(leftKey);
                    break;
                }
            }
        }
        return new ArrayList<>(columns);
    }

    private static boolean rowsEqual(Map<String, Object> leftRow, Map<String, Object> rightRow, List<String> columns) {
        if (columns.isEmpty()) {
            return Objects.equals(leftRow, rightRow);
        }
        for (String column : columns) {
            Object leftValue = leftRow == null ? null : leftRow.get(column);
            Object rightValue = resolveColumnValue(rightRow, column);
            if (!valuesEqual(leftValue, rightValue)) {
                return false;
            }
        }
        return true;
    }

    private static Object resolveColumnValue(Map<String, Object> row, String column) {
        if (row == null || column == null) {
            return null;
        }
        if (row.containsKey(column)) {
            return row.get(column);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (columnNamesMatch(column, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean columnNamesMatch(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }
        String expectedLower = expected.toLowerCase(Locale.ROOT);
        String actualLower = actual.toLowerCase(Locale.ROOT);
        return actualLower.endsWith("." + expectedLower);
    }

    /**
     * Compares cell values, treating numeric types with the same magnitude as equal (V1 Long vs V2 Integer, etc.).
     */
    private static boolean valuesEqual(Object leftValue, Object rightValue) {
        if (Objects.equals(leftValue, rightValue)) {
            return true;
        }
        if (leftValue instanceof Number leftNumber && rightValue instanceof Number rightNumber) {
            return new BigDecimal(leftNumber.toString()).compareTo(new BigDecimal(rightNumber.toString())) == 0;
        }
        if (leftValue != null && rightValue != null) {
            try {
                return new BigDecimal(leftValue.toString()).compareTo(new BigDecimal(rightValue.toString())) == 0;
            }
            catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return false;
    }
}
