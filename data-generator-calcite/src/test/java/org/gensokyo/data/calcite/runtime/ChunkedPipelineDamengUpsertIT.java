/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.support.UpsertParitySupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Optional Dameng MERGE upsert integration test against a real external JDBC endpoint (DIAL-01).
 *
 * <p>Skipped by default via the class-level {@link org.gensokyo.data.calcite.support.DamengTestSupport}
 * gate; enable with {@code -Ddm.it=true} or {@code DG_DM_IT=true}. Once enabled, this test requires
 * three connection environment variables:
 *
 * <ul>
 *   <li>{@code DG_DM_JDBC_URL} — Dameng JDBC URL</li>
 *   <li>{@code DG_DM_USER} — database user</li>
 *   <li>{@code DG_DM_PASSWORD} — database password</li>
 * </ul>
 *
 * <p>If the opt-in flag is on but any of these variables is missing or blank, the test fails the
 * build with {@link IllegalStateException} rather than skipping — a misconfigured opt-in run must
 * never report as green. See the recipe in {@code docs/template-v2-jdbc-sink-guide.md} for the full
 * setup. The default CI bar for Dameng MERGE SQL generation remains the unit-level
 * {@code JdbcSinkSqlBuilderTests}, which do not require a live Dameng host.
 *
 * @author Gensokyo
 * @since 2026-07-28
 */
@EnabledIf("org.gensokyo.data.calcite.support.DamengTestSupport#damengItEnabled")
class ChunkedPipelineDamengUpsertIT {

    /**
     * Proves CHUNKED JDBC upsert idempotency against a real Dameng host by delegating to the shared
     * {@link UpsertParitySupport} helper used by the PostgreSQL, MySQL, Kingbase, and HighGo ITs.
     */
    @Test
    void chunkedUpsertDamengMergeIsIdempotent() {
        UpsertParitySupport.assertUpsertIdempotent(
                requireEnv("DG_DM_JDBC_URL"),
                requireEnv("DG_DM_USER"),
                requireEnv("DG_DM_PASSWORD"),
                "dm.jdbc.driver.DmDriver",
                "dameng");
    }

    /**
     * Reads a required connection environment variable, failing hard (never skipping) when it is
     * absent or blank. The exception message never includes the variable's value so a real
     * credential cannot leak into Surefire output or CI logs.
     */
    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Dameng live IT is enabled (DG_DM_IT/dm.it) but required environment variable "
                            + name + " is missing or blank; see the Dameng live IT recipe in "
                            + "docs/template-v2-jdbc-sink-guide.md before enabling this test.");
        }
        return value;
    }
}
