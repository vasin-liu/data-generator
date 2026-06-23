/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.udf.UdfRecord;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * Per-version console view of a UDF registry entry (D-14).
 *
 * <p>Projects a {@link UdfRecord} into the wire shape consumed by the operator console. The artifact
 * {@code payload} bytes are intentionally never mapped so list/history responses cannot leak code-bearing
 * content; only lifecycle metadata and timestamps are exposed.
 *
 * @param udfId        reverse-DNS UDF identifier
 * @param version      semver version
 * @param type         stable wire type name ({@code java-plugin}/{@code script}/{@code sql})
 * @param state        lifecycle state, lowercased ({@code draft}/{@code published}/{@code deprecated})
 * @param registeredAt draft registration timestamp
 * @param publishedAt  publish timestamp, or {@code null} while in draft
 * @param deprecatedAt deprecation timestamp, or {@code null} until deprecated
 * @param metadata     non-sensitive metadata (e.g. {@code sqlName} for SQL UDFs)
 * @author Gensokyo
 * @since 2026-06-18
 */
public record UdfVersionView(
        String udfId,
        String version,
        String type,
        String state,
        Instant registeredAt,
        Instant publishedAt,
        Instant deprecatedAt,
        Map<String, String> metadata) {

    /**
     * Maps a registry record to its console view, dropping the payload bytes (D-14).
     *
     * @param record source registry record
     * @return per-version view
     */
    public static UdfVersionView from(UdfRecord record) {
        return new UdfVersionView(
                record.udfId(),
                record.version(),
                record.type().jsonName(),
                record.state().name().toLowerCase(Locale.ROOT),
                record.registeredAt(),
                record.publishedAt(),
                record.deprecatedAt(),
                record.metadata());
    }
}
