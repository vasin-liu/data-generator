/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.geo.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gensokyo.data.geo.GeoAssetResolver;

/**
 * Resolves GeoJSON resource paths ({@code classpath:...}, filesystem, or {@code asset:{uuid}}).
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public final class GeoResourceResolver {

    /** Wire-format prefix for metadata DB geo asset references (D-03/D-09). */
    public static final String ASSET_PREFIX = "asset:";

    private static final String CLASSPATH_PREFIX = "classpath:";

    private GeoResourceResolver() {
    }

    /**
     * Reads the entire GeoJSON file as UTF-8 text (classpath or filesystem only).
     *
     * @param location classpath or file path
     * @return file contents
     * @throws IOException when the resource cannot be read
     */
    public static String readUtf8(String location) throws IOException {
        return readUtf8(location, null);
    }

    /**
     * Reads the entire GeoJSON resource as UTF-8 text.
     *
     * @param location classpath, file, or {@code asset:{uuid}} location
     * @param assets   resolver for {@code asset:} locations; required when location uses that prefix
     * @return file contents
     * @throws IOException when the resource cannot be read
     */
    public static String readUtf8(String location, GeoAssetResolver assets) throws IOException {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("GeoJSON location must not be blank");
        }
        String trimmed = location.strip();
        if (trimmed.startsWith(ASSET_PREFIX)) {
            if (assets == null) {
                throw new IllegalArgumentException(
                        "Geo asset resolver is required for location [" + trimmed + "]");
            }
            String assetId = trimmed.substring(ASSET_PREFIX.length()).strip();
            if (assetId.isEmpty()) {
                throw new IllegalArgumentException("Geo asset id must not be blank in location [" + trimmed + "]");
            }
            return assets.resolveUtf8(assetId);
        }
        if (trimmed.startsWith(CLASSPATH_PREFIX)) {
            String resourcePath = trimmed.substring(CLASSPATH_PREFIX.length());
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IllegalArgumentException("Classpath GeoJSON not found: " + resourcePath);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return Files.readString(Path.of(trimmed));
    }
}
