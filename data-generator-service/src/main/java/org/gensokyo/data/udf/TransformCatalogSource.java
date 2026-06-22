/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

import org.gensokyo.data.api.console.dto.TransformCatalogEntryView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Merges authored built-in operator descriptors with published UDFs into one unified transform catalog
 * (D-06). Non-published UDFs are excluded, and internal {@code V2_*} scalar functions are never emitted
 * (D-12).
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
public class TransformCatalogSource {

    /** Source discriminator value for published UDF entries (D-06). */
    public static final String KIND_UDF = "UDF";

    private final UdfRegistryService udfRegistryService;

    /**
     * @param udfRegistryService facade over the UDF registry used to project published entries
     */
    public TransformCatalogSource(UdfRegistryService udfRegistryService) {
        this.udfRegistryService = udfRegistryService;
    }

    /**
     * Returns the unified catalog: built-in operators plus published UDFs, optionally filtered by kind.
     *
     * @param kindFilter optional {@code BUILTIN}/{@code UDF} filter (case-insensitive)
     * @return catalog entries matching the filter
     * @throws IllegalArgumentException when {@code kindFilter} is present but not a known kind
     */
    public List<TransformCatalogEntryView> entries(Optional<String> kindFilter) {
        String normalizedKind = normalizeKind(kindFilter);
        List<TransformCatalogEntryView> entries = new ArrayList<>(BuiltinTransformCatalog.entries());
        for (UdfRecord record : udfRegistryService.list(Optional.empty())) {
            // Only published UDFs are discoverable; drafts/deprecated stay hidden (D-06).
            if (record.state() == UdfLifecycleState.PUBLISHED) {
                entries.add(toEntry(record));
            }
        }
        if (normalizedKind == null) {
            return List.copyOf(entries);
        }
        return entries.stream()
                .filter(entry -> entry.kind().equals(normalizedKind))
                .toList();
    }

    private static TransformCatalogEntryView toEntry(UdfRecord record) {
        String sqlName = sqlNameOf(record);
        String description = "Published " + record.type().jsonName() + " UDF";
        return new TransformCatalogEntryView(record.udfId(), KIND_UDF, description, List.of(), null, sqlName);
    }

    private static String sqlNameOf(UdfRecord record) {
        // Java plugins are not SQL-callable; only SQL/script UDFs carry a sqlName in their payload envelope.
        if (record.type() != UdfType.SQL && record.type() != UdfType.SCRIPT) {
            return null;
        }
        return ScriptUdfPayload.parse(record.payload()).sqlName();
    }

    private static String normalizeKind(Optional<String> kindFilter) {
        if (kindFilter.isEmpty() || kindFilter.get().isBlank()) {
            return null;
        }
        String kind = kindFilter.get().trim().toUpperCase(Locale.ROOT);
        if (!kind.equals(BuiltinTransformCatalog.KIND_BUILTIN) && !kind.equals(KIND_UDF)) {
            throw new IllegalArgumentException("Unknown transform kind: " + kindFilter.get());
        }
        return kind;
    }
}
