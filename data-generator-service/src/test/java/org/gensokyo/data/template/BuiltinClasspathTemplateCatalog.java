/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Discovers built-in templates shipped under {@code classpath:template/} (recursive YAML).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class BuiltinClasspathTemplateCatalog {

    private static final String CLASSPATH_PATTERN = "classpath*:/template/**/*.yaml";

    private BuiltinClasspathTemplateCatalog() {
    }

    /**
     * One built-in template fixture from the service module resources.
     *
     * @param relativePath path under {@code template/} (e.g. {@code demo/00_常规样例.yaml})
     * @param yaml         raw YAML text
     */
    public record Fixture(String relativePath, String yaml) {

        /**
         * Stable display name for test output.
         *
         * @return relative path
         */
        public String displayName() {
            return relativePath;
        }

        /**
         * Stable synthetic database id derived from the relative path (collision-resistant band).
         *
         * @return id in {@code 920_000_000} range
         */
        public long stableTemplateId() {
            int hash = Objects.hash(relativePath);
            return 920_000_000L + Math.floorMod(hash, 9_999_999);
        }

        /**
         * Inventory-style id for migration evidence tables.
         *
         * @return {@code builtin-} plus slugged path
         */
        public String inventoryId() {
            String slug = relativePath
                    .replace('\\', '/')
                    .replace('/', '-')
                    .replace(".yaml", "")
                    .replace(".yml", "")
                    .toLowerCase(Locale.ROOT);
            return "builtin-" + slug;
        }
    }

    /**
     * Loads every readable built-in template YAML from the classpath.
     *
     * @return sorted fixtures (skips {@code !} prefixed draft files)
     * @throws IllegalStateException when classpath scanning fails
     */
    public static List<Fixture> loadAll() {
        List<Fixture> fixtures = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(CLASSPATH_PATTERN);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                String filename = resource.getFilename();
                if (filename == null || filename.startsWith("!")) {
                    continue;
                }
                String relativePath = toRelativePath(resource);
                // Greenfield V2 scenarios are validated by V2ScenarioTemplateIT, not V1 migration census.
                if (relativePath.startsWith("v2-scenarios/")) {
                    continue;
                }
                String yaml = resource.getContentAsString(StandardCharsets.UTF_8);
                fixtures.add(new Fixture(relativePath, yaml));
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load built-in templates from classpath", e);
        }
        fixtures.sort(Comparator.comparing(Fixture::relativePath));
        return fixtures;
    }

    /**
     * Finds a fixture by exact relative path.
     *
     * @param relativePath path under {@code template/}
     * @return fixture
     * @throws IllegalArgumentException when not found
     */
    public static Fixture require(String relativePath) {
        return loadAll().stream()
                .filter(f -> f.relativePath().equals(relativePath))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Built-in template not found: " + relativePath));
    }

    private static String toRelativePath(Resource resource) throws IOException {
        String url = resource.getURI().toString();
        int marker = url.indexOf("/template/");
        if (marker < 0) {
            return Objects.requireNonNull(resource.getFilename(), "filename");
        }
        return url.substring(marker + "/template/".length());
    }
}
