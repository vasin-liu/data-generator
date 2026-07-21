/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdbcDriverPresetCatalog}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
class JdbcDriverPresetCatalogTests {

    @Test
    void resolveDriverClassCandidates_includesKingbase9Alternates() {
        List<String> candidates = JdbcDriverPresetCatalog.resolveDriverClassCandidates(
                "com.kingbase9.Driver", "jdbc:kingbase9://localhost:54321/db");
        assertThat(candidates)
                .containsExactly(
                        "com.kingbase9.Driver",
                        "com.kingbase8.Driver",
                        "com.kingbase.Driver");
    }

    @Test
    void resolveDriverClassCandidates_includesClickHouseLegacyAlternate() {
        List<String> candidates = JdbcDriverPresetCatalog.resolveDriverClassCandidates(
                "com.clickhouse.jdbc.ClickHouseDriver", "jdbc:clickhouse://localhost:8123/default");
        assertThat(candidates)
                .contains("com.clickhouse.jdbc.ClickHouseDriver", "ru.yandex.clickhouse.ClickHouseDriver");
    }

    @Test
    void resolveDriverClassCandidates_matchesByJdbcUrlWhenClassUnknown() {
        List<String> candidates = JdbcDriverPresetCatalog.resolveDriverClassCandidates(
                "com.example.CustomDriver", "jdbc:dm://host:5236");
        assertThat(candidates).contains("com.example.CustomDriver", "dm.jdbc.driver.DmDriver");
    }

    @Test
    void all_presetsHaveUniqueIds() {
        List<String> ids = JdbcDriverPresetCatalog.all().stream().map(JdbcDriverPreset::id).toList();
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void resolveBundleKey_forMysqlUrl() {
        assertThat(JdbcDriverPresetCatalog.resolveBundleKey("com.mysql.cj.jdbc.Driver", "jdbc:mysql://h/db"))
                .contains("mysql");
    }

    @Test
    void resolveDriverClassCandidates_resolvesHighGoUrl() {
        List<String> candidates = JdbcDriverPresetCatalog.resolveDriverClassCandidates(
                null, "jdbc:highgo://localhost:5866/highgo");
        assertThat(candidates).containsExactly("com.highgo.jdbc.Driver");
    }

    @Test
    void resolveDriverClassCandidates_resolvesDmUrl() {
        List<String> candidates = JdbcDriverPresetCatalog.resolveDriverClassCandidates(
                null, "jdbc:dm://host:5236");
        assertThat(candidates).containsExactly("dm.jdbc.driver.DmDriver");
    }

    @Test
    void resolveBundleKey_forClickHouseUrl() {
        assertThat(JdbcDriverPresetCatalog.resolveBundleKey(
                        "com.clickhouse.jdbc.ClickHouseDriver", "jdbc:clickhouse://localhost:8123/default"))
                .contains("clickhouse");
    }

    @Test
    void postgresql16Preset_hasCompleteFields() {
        JdbcDriverPreset preset = JdbcDriverPresetCatalog.byId("postgresql16").orElseThrow();
        assertThat(preset.primaryDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(preset.urlTemplate()).isEqualTo("jdbc:postgresql://localhost:5432/postgres");
        assertThat(preset.groupKey()).isEqualTo("postgresql");
        assertThat(preset.bundleKey()).isEqualTo("postgresql");
    }

    @Test
    void all_phase9EngineGroups_haveNonBlankUrlAndDriver() {
        List<String> phase9Groups = List.of("dm", "kingbase", "highgo", "clickhouse", "postgresql");
        for (String groupKey : phase9Groups) {
            assertThat(JdbcDriverPresetCatalog.all())
                    .anySatisfy(preset -> {
                        assertThat(preset.groupKey()).isEqualTo(groupKey);
                        assertThat(preset.primaryDriverClassName()).isNotBlank();
                        assertThat(preset.urlTemplate()).isNotBlank();
                    });
        }
    }
}
