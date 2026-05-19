/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Pure classification of dual-run compare metrics into {@link MigrationClassification}.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class MigrationClassificationRules {

    /** Sample match rate at or above which an exact classification is allowed (when counts and warnings permit). */
    public static final double SAMPLE_MATCH_EXACT = 0.999;

    /** Sample match rate below which compare is blocked. */
    public static final double SAMPLE_MATCH_BLOCKED = 0.95;

    private static final String INFO_WARNING_PREFIX = "info:";

    private MigrationClassificationRules() {
    }

    /**
     * Classifies migration compare outcome from row counts, sample match rate, and analyzer warnings.
     *
     * @param v1RowCount      V1 pipeline row count
     * @param v2RowCount      V2 pipeline row count
     * @param sampleMatchRate sample row match rate in {@code [0, 1]}
     * @param warnings        analyzer or compare warnings (may be empty)
     * @return migration classification
     */
    public static MigrationClassification classify(
            long v1RowCount,
            long v2RowCount,
            double sampleMatchRate,
            List<String> warnings) {
        List<String> safeWarnings = warnings == null ? List.of() : warnings;

        if (sampleMatchRate < SAMPLE_MATCH_BLOCKED) {
            return MigrationClassification.BLOCKED;
        }

        boolean countsMatch = v1RowCount == v2RowCount;
        boolean materialWarnings = hasMaterialWarnings(safeWarnings);

        if (!countsMatch) {
            // Significant count mismatch: high sample agreement still needs review, not exact.
            return sampleMatchRate >= SAMPLE_MATCH_EXACT
                    ? MigrationClassification.APPROXIMATE
                    : MigrationClassification.BLOCKED;
        }

        if (sampleMatchRate >= SAMPLE_MATCH_EXACT && !materialWarnings) {
            return MigrationClassification.EXACT;
        }

        return MigrationClassification.APPROXIMATE;
    }

    private static boolean hasMaterialWarnings(List<String> warnings) {
        for (String warning : warnings) {
            if (warning == null || warning.isBlank()) {
                continue;
            }
            String normalized = warning.strip().toLowerCase(Locale.ROOT);
            if (!normalized.startsWith(INFO_WARNING_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Maps a compare classification to a promote/review recommendation token.
     *
     * @param classification compare classification
     * @return {@code accept}, {@code accept_with_review}, or {@code reject}
     */
    public static String recommendationFor(MigrationClassification classification) {
        Objects.requireNonNull(classification, "classification");
        return switch (classification) {
            case EXACT -> "accept";
            case APPROXIMATE, ADAPTED -> "accept_with_review";
            case BLOCKED, COMPATIBILITY_ONLY, UNCLASSIFIED -> "reject";
        };
    }
}
