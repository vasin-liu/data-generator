/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.udf.UdfRecord;

import java.util.Comparator;
import java.util.List;

/**
 * Console list view of one UDF identity and its full version history (D-14).
 *
 * <p>Groups every {@link UdfRecord} sharing a {@code udfId} into a single entry so the operator console can
 * render version history (D-08) without exposing payload bytes. The group {@code type} is derived from the
 * member records (all versions of one id share a type).
 *
 * @param udfId    reverse-DNS UDF identifier
 * @param type     stable wire type name shared by all versions, or {@code null} when the group is empty
 * @param versions per-version views, ordered ascending by version
 * @author Gensokyo
 * @since 2026-06-18
 */
public record UdfGroupView(String udfId, String type, List<UdfVersionView> versions) {

    /**
     * Builds a grouped view from all records belonging to a single {@code udfId} (D-14).
     *
     * @param udfId         shared UDF identifier
     * @param recordsForId  records for this id (any lifecycle state); never {@code null}
     * @return grouped view with versions sorted ascending
     */
    public static UdfGroupView of(String udfId, List<UdfRecord> recordsForId) {
        // All versions of one udfId share a type; derive it from the first record (null when empty).
        String type = recordsForId.isEmpty() ? null : recordsForId.get(0).type().jsonName();
        List<UdfVersionView> versions = recordsForId.stream()
                .sorted(Comparator.comparing(UdfRecord::version, UdfGroupView::compareSemver))
                .map(UdfVersionView::from)
                .toList();
        return new UdfGroupView(udfId, type, versions);
    }

    private static int compareSemver(String left, String right) {
        // Numeric semver ordering so 1.10.0 sorts after 1.9.0; falls back to string compare off-format.
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        for (int i = 0; i < Math.min(l.length, r.length); i++) {
            try {
                int cmp = Integer.compare(Integer.parseInt(l[i]), Integer.parseInt(r[i]));
                if (cmp != 0) {
                    return cmp;
                }
            } catch (NumberFormatException notNumeric) {
                int cmp = l[i].compareTo(r[i]);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }
        return Integer.compare(l.length, r.length);
    }
}
