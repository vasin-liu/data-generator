/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.model.v2.MaterializationPolicyVO;
import org.gensokyo.data.model.v2.Row;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Applies {@link MaterializationPolicyVO} to a materialized row list.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class MaterializationPolicySupport {
    private static final long DEFAULT_SEED = 0L;

    private MaterializationPolicySupport() {
    }

    /**
     * Applies a materialization policy to source rows.
     *
     * @param sourceRows rows produced by the underlying source before policy application
     * @param policy     materialization policy; {@code null} returns {@code sourceRows} unchanged
     * @return policy-adjusted rows
     */
    public static List<Row> apply(List<Row> sourceRows, MaterializationPolicyVO policy) {
        if (policy == null || policy.getMode() == null || policy.getMode().isBlank()) {
            return sourceRows;
        }
        return switch (normalizeMode(policy.getMode())) {
            case "ORDERED" -> limit(sourceRows, policy.getLimit());
            case "LIMIT" -> requireLimit(sourceRows, policy.getLimit());
            case "ONCE" -> onceRows(sourceRows, policy.getLimit());
            case "EQUAL" -> limit(shuffle(sourceRows, seed(policy)), policy.getLimit());
            case "WEIGHTED" -> limit(weightedRows(sourceRows, policy), policy.getLimit());
            default -> throw new IllegalArgumentException("Unsupported materialization policy mode: " + policy.getMode());
        };
    }

    /**
     * Normalizes a policy mode token for comparison.
     *
     * @param mode raw mode from template JSON
     * @return upper-case trimmed mode
     */
    public static String normalizeMode(String mode) {
        return mode.trim().toUpperCase(Locale.ROOT);
    }

    private static List<Row> onceRows(List<Row> sourceRows, Integer limit) {
        List<Row> unique = new ArrayList<>();
        Set<Row> seen = new LinkedHashSet<>();
        for (Row row : sourceRows) {
            if (seen.add(row)) {
                unique.add(row);
            }
        }
        return limit(unique, limit);
    }

    private static List<Row> weightedRows(List<Row> sourceRows, MaterializationPolicyVO policy) {
        List<Integer> weights = policy.getWeights();
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("WEIGHTED materialization policy requires non-empty weights");
        }
        if (weights.size() != sourceRows.size()) {
            throw new IllegalArgumentException("WEIGHTED materialization policy weights size ["
                    + weights.size() + "] must match source row count [" + sourceRows.size() + "]");
        }
        List<Row> expanded = new ArrayList<>();
        for (int index = 0; index < sourceRows.size(); index++) {
            int weight = weights.get(index);
            if (weight <= 0) {
                throw new IllegalArgumentException("WEIGHTED materialization policy weight at index ["
                        + index + "] must be positive");
            }
            Row row = sourceRows.get(index);
            for (int copy = 0; copy < weight; copy++) {
                expanded.add(row);
            }
        }
        return shuffle(expanded, seed(policy));
    }

    private static List<Row> shuffle(List<Row> sourceRows, long seed) {
        List<Row> rows = new ArrayList<>(sourceRows);
        Collections.shuffle(rows, new Random(seed));
        return List.copyOf(rows);
    }

    private static List<Row> requireLimit(List<Row> sourceRows, Integer limit) {
        if (limit == null) {
            throw new IllegalArgumentException("LIMIT materialization policy requires a positive limit");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("LIMIT materialization policy limit must be positive");
        }
        return limit(sourceRows, limit);
    }

    private static List<Row> limit(List<Row> sourceRows, Integer limit) {
        if (limit == null) {
            return sourceRows;
        }
        if (limit < 0) {
            throw new IllegalArgumentException("Materialization policy limit must be greater than or equal to 0");
        }
        if (limit >= sourceRows.size()) {
            return sourceRows;
        }
        return List.copyOf(sourceRows.subList(0, limit));
    }

    private static long seed(MaterializationPolicyVO policy) {
        return Objects.requireNonNullElse(policy.getSeed(), DEFAULT_SEED);
    }
}
