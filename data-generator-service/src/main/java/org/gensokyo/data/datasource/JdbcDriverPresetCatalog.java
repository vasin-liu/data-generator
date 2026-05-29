/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Canonical JDBC driver presets and driver-class resolution for datasource registration.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class JdbcDriverPresetCatalog {

    private static final List<JdbcDriverPreset> PRESETS = List.of(
            preset(
                    "dm8",
                    "dm",
                    "dm",
                    "datasources.driver.dm8",
                    "dm.jdbc.driver.DmDriver",
                    List.of(),
                    "jdbc:dm://localhost:5236?schema=YOUR_SCHEMA"),
            preset(
                    "kingbase8",
                    "kingbase",
                    "kingbase8",
                    "datasources.driver.kingbase8",
                    "com.kingbase8.Driver",
                    List.of("com.kingbase.Driver"),
                    "jdbc:kingbase8://localhost:54321/YOUR_DATABASE"),
            preset(
                    "kingbase9",
                    "kingbase",
                    "kingbase9",
                    "datasources.driver.kingbase9",
                    "com.kingbase9.Driver",
                    List.of("com.kingbase8.Driver", "com.kingbase.Driver"),
                    "jdbc:kingbase9://localhost:54321/YOUR_DATABASE"),
            preset(
                    "highgo",
                    "highgo",
                    "highgo",
                    "datasources.driver.highgo",
                    "com.highgo.jdbc.Driver",
                    List.of(),
                    "jdbc:highgo://localhost:5866/highgo"),
            preset(
                    "clickhouse20",
                    "clickhouse",
                    "clickhouse",
                    "datasources.driver.clickhouse20",
                    "com.clickhouse.jdbc.ClickHouseDriver",
                    List.of("ru.yandex.clickhouse.ClickHouseDriver"),
                    "jdbc:clickhouse://localhost:8123/default"),
            preset(
                    "clickhouse22",
                    "clickhouse",
                    "clickhouse",
                    "datasources.driver.clickhouse22",
                    "com.clickhouse.jdbc.ClickHouseDriver",
                    List.of("ru.yandex.clickhouse.ClickHouseDriver"),
                    "jdbc:clickhouse://localhost:8123/default"),
            preset(
                    "clickhouse24",
                    "clickhouse",
                    "clickhouse",
                    "datasources.driver.clickhouse24",
                    "com.clickhouse.jdbc.ClickHouseDriver",
                    List.of("ru.yandex.clickhouse.ClickHouseDriver"),
                    "jdbc:clickhouse://localhost:8123/default"),
            preset(
                    "clickhouse26",
                    "clickhouse",
                    "clickhouse",
                    "datasources.driver.clickhouse26",
                    "com.clickhouse.jdbc.ClickHouseDriver",
                    List.of("ru.yandex.clickhouse.ClickHouseDriver"),
                    "jdbc:clickhouse://localhost:8123/default"),
            preset(
                    "postgresql10",
                    "postgresql",
                    "postgresql",
                    "datasources.driver.postgresql10",
                    "org.postgresql.Driver",
                    List.of(),
                    "jdbc:postgresql://localhost:5432/postgres"),
            preset(
                    "postgresql12",
                    "postgresql",
                    "postgresql",
                    "datasources.driver.postgresql12",
                    "org.postgresql.Driver",
                    List.of(),
                    "jdbc:postgresql://localhost:5432/postgres"),
            preset(
                    "postgresql14",
                    "postgresql",
                    "postgresql",
                    "datasources.driver.postgresql14",
                    "org.postgresql.Driver",
                    List.of(),
                    "jdbc:postgresql://localhost:5432/postgres"),
            preset(
                    "postgresql16",
                    "postgresql",
                    "postgresql",
                    "datasources.driver.postgresql16",
                    "org.postgresql.Driver",
                    List.of(),
                    "jdbc:postgresql://localhost:5432/postgres"),
            preset(
                    "postgresql18",
                    "postgresql",
                    "postgresql",
                    "datasources.driver.postgresql18",
                    "org.postgresql.Driver",
                    List.of(),
                    "jdbc:postgresql://localhost:5432/postgres"),
            preset(
                    "mysql5",
                    "mysql",
                    "mysql",
                    "datasources.driver.mysql5",
                    "com.mysql.jdbc.Driver",
                    List.of(),
                    "jdbc:mysql://localhost:3306/YOUR_DATABASE?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8"),
            preset(
                    "mysql57",
                    "mysql",
                    "mysql",
                    "datasources.driver.mysql57",
                    "com.mysql.cj.jdbc.Driver",
                    List.of("com.mysql.jdbc.Driver"),
                    "jdbc:mysql://localhost:3306/YOUR_DATABASE?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8"),
            preset(
                    "mysql8",
                    "mysql",
                    "mysql",
                    "datasources.driver.mysql8",
                    "com.mysql.cj.jdbc.Driver",
                    List.of("com.mysql.jdbc.Driver"),
                    "jdbc:mysql://localhost:3306/YOUR_DATABASE?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8"),
            preset(
                    "mysql84",
                    "mysql",
                    "mysql",
                    "datasources.driver.mysql84",
                    "com.mysql.cj.jdbc.Driver",
                    List.of("com.mysql.jdbc.Driver"),
                    "jdbc:mysql://localhost:3306/YOUR_DATABASE?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8"),
            preset(
                    "mysql9",
                    "mysql",
                    "mysql",
                    "datasources.driver.mysql9",
                    "com.mysql.cj.jdbc.Driver",
                    List.of("com.mysql.jdbc.Driver"),
                    "jdbc:mysql://localhost:3306/YOUR_DATABASE?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8"));

    private JdbcDriverPresetCatalog() {
    }

    /**
     * @return immutable preset list for console API
     */
    public static List<JdbcDriverPreset> all() {
        return PRESETS;
    }

    /**
     * @param id preset id
     * @return matching preset when present
     */
    public static Optional<JdbcDriverPreset> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return PRESETS.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /**
     * Resolves driver classes to try: requested name first, then catalog alternates for a matching preset.
     *
     * @param driverClassName user-selected or persisted driver class
     * @param jdbcUrl         JDBC URL (used to match preset when class is shared)
     * @return ordered unique candidate class names
     */
    /**
     * @param driverClassName configured driver class
     * @param jdbcUrl         JDBC URL
     * @return bundled jar directory key when a preset matches
     */
    public static Optional<String> resolveBundleKey(String driverClassName, String jdbcUrl) {
        return findMatchingPreset(driverClassName, jdbcUrl).map(JdbcDriverPreset::bundleKey);
    }

    /**
     * @param driverClassName configured driver class
     * @param jdbcUrl         JDBC URL
     * @return ordered unique candidate class names
     */
    public static List<String> resolveDriverClassCandidates(String driverClassName, String jdbcUrl) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (driverClassName != null && !driverClassName.isBlank()) {
            ordered.add(driverClassName.trim());
        }
        findMatchingPreset(driverClassName, jdbcUrl)
                .ifPresent(preset -> ordered.addAll(preset.driverClassCandidates()));
        if (ordered.isEmpty() && driverClassName != null && !driverClassName.isBlank()) {
            ordered.add(driverClassName.trim());
        }
        return List.copyOf(ordered);
    }

    /**
     * @param primary primary driver class
     * @param alternates alternate classes
     * @return merged list without duplicates (primary first)
     */
    static List<String> mergeCandidates(String primary, List<String> alternates) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (primary != null && !primary.isBlank()) {
            ordered.add(primary.trim());
        }
        if (alternates != null) {
            for (String alt : alternates) {
                if (alt != null && !alt.isBlank()) {
                    ordered.add(alt.trim());
                }
            }
        }
        return List.copyOf(ordered);
    }

    private static Optional<JdbcDriverPreset> findMatchingPreset(String driverClassName, String jdbcUrl) {
        String urlLower = jdbcUrl == null ? "" : jdbcUrl.toLowerCase(Locale.ROOT);
        JdbcDriverPreset byClass = null;
        JdbcDriverPreset byUrl = null;
        for (JdbcDriverPreset preset : PRESETS) {
            if (matchesClass(preset, driverClassName)) {
                byClass = preset;
            }
            if (!urlLower.isBlank() && matchesUrl(preset, urlLower)) {
                byUrl = preset;
            }
        }
        if (byClass != null) {
            return Optional.of(byClass);
        }
        return Optional.ofNullable(byUrl);
    }

    private static boolean matchesClass(JdbcDriverPreset preset, String driverClassName) {
        if (driverClassName == null || driverClassName.isBlank()) {
            return false;
        }
        String requested = driverClassName.trim();
        if (requested.equals(preset.primaryDriverClassName())) {
            return true;
        }
        return preset.alternateDriverClassNames().stream().anyMatch(requested::equals);
    }

    private static boolean matchesUrl(JdbcDriverPreset preset, String urlLower) {
        return switch (preset.groupKey()) {
            case "dm" -> urlLower.contains(":dm:");
            case "kingbase" -> urlLower.contains(":kingbase");
            case "highgo" -> urlLower.contains(":highgo:");
            case "clickhouse" -> urlLower.contains(":clickhouse:") || urlLower.contains(":ch:");
            case "postgresql" -> urlLower.contains(":postgresql:");
            case "mysql" -> urlLower.contains(":mysql:");
            default -> false;
        };
    }

    private static JdbcDriverPreset preset(
            String id,
            String groupKey,
            String bundleKey,
            String labelKey,
            String primaryDriver,
            List<String> alternates,
            String urlTemplate) {
        return new JdbcDriverPreset(
                id, groupKey, bundleKey, labelKey, primaryDriver, List.copyOf(alternates), urlTemplate);
    }
}
