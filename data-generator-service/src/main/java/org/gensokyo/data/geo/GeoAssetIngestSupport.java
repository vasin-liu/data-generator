/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo;

import org.gensokyo.data.geo.io.GeoFeature;
import org.gensokyo.data.geo.io.GeoJsonLoader;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates uploaded GeoJSON bytes and derives ingest metadata before persistence (D-07).
 *
 * @author Gensokyo
 * @since 2026-08-01
 */
public final class GeoAssetIngestSupport {

    private static final String CONTENT_TYPE = "application/geo+json";

    private GeoAssetIngestSupport() {
    }

    /**
     * Validated ingest payload ready for {@link org.gensokyo.data.model.po.GeoAssetPO} mapping.
     *
     * @param geoJsonText      normalized GeoJSON text
     * @param featureCount     parsed feature count
     * @param minLon           bbox minimum longitude
     * @param minLat           bbox minimum latitude
     * @param maxLon           bbox maximum longitude
     * @param maxLat           bbox maximum latitude
     * @param geometrySummary  optional geometry-type summary
     * @param contentSha256    SHA-256 hex digest of UTF-8 bytes
     * @param contentType      MIME type
     */
    public record IngestResult(
            String geoJsonText,
            int featureCount,
            double minLon,
            double minLat,
            double maxLon,
            double maxLat,
            String geometrySummary,
            String contentSha256,
            String contentType) {
    }

    /**
     * Parses and validates GeoJSON bytes against configured limits.
     *
     * @param utf8Bytes   uploaded file bytes
     * @param maxBytes    maximum allowed byte length
     * @param maxFeatures maximum allowed feature count
     * @return validated text and derived metadata
     * @throws IllegalArgumentException when size, feature count, or GeoJSON shape is invalid
     */
    public static IngestResult ingest(byte[] utf8Bytes, long maxBytes, int maxFeatures) {
        if (utf8Bytes == null || utf8Bytes.length == 0) {
            throw new IllegalArgumentException("GeoJSON upload must not be empty");
        }
        if (utf8Bytes.length > maxBytes) {
            throw new IllegalArgumentException(
                    "GeoJSON exceeds max size of " + maxBytes + " bytes (got " + utf8Bytes.length + ")");
        }
        String text = new String(utf8Bytes, StandardCharsets.UTF_8).strip();
        if (text.isBlank()) {
            throw new IllegalArgumentException("GeoJSON upload must not be blank");
        }

        List<GeoFeature> features = parseFeatures(text);
        if (features.isEmpty()) {
            throw new IllegalArgumentException("GeoJSON must contain at least one feature");
        }
        if (features.size() > maxFeatures) {
            throw new IllegalArgumentException(
                    "GeoJSON feature count exceeds max of " + maxFeatures + " (got " + features.size() + ")");
        }

        Envelope envelope = new Envelope();
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        for (GeoFeature feature : features) {
            Geometry geometry = feature.geometry();
            envelope.expandToInclude(geometry.getEnvelopeInternal());
            String typeName = geometry.getGeometryType();
            typeCounts.merge(typeName, 1, Integer::sum);
        }
        if (envelope.isNull()) {
            throw new IllegalArgumentException("GeoJSON geometries have no measurable envelope");
        }

        String geometrySummary = typeCounts.entrySet().stream()
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
        return new IngestResult(
                text,
                features.size(),
                envelope.getMinX(),
                envelope.getMinY(),
                envelope.getMaxX(),
                envelope.getMaxY(),
                geometrySummary,
                sha256Hex(utf8Bytes),
                CONTENT_TYPE);
    }

    /**
     * Validates root type via {@link GeoJsonLoader} (Feature / FeatureCollection only).
     */
    private static List<GeoFeature> parseFeatures(String geoJsonText) {
        Path temp = null;
        try {
            temp = Files.createTempFile("geo-ingest-", ".geojson");
            Files.writeString(temp, geoJsonText, StandardCharsets.UTF_8);
            return GeoJsonLoader.loadFeatureCollection(temp.toString());
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to parse GeoJSON: " + ex.getMessage(), ex);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // Best-effort temp cleanup; ingest outcome already determined.
                }
            }
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
