/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.support;

/**
 * Opt-in gate for the {@code ChunkedPipelineDamengUpsertIT} live Dameng integration test (DIAL-01).
 *
 * <p>This class answers only "is opt-in on?" — it does not validate the connection itself. Enable
 * the flag with either:
 *
 * <ul>
 *   <li>JVM: {@code -Ddm.it=true}</li>
 *   <li>Environment: {@code DG_DM_IT=true}</li>
 * </ul>
 *
 * <p>When enabled, the IT additionally requires three connection environment variables:
 * {@code DG_DM_JDBC_URL}, {@code DG_DM_USER}, and {@code DG_DM_PASSWORD}. If the flag is on but any
 * of these is missing, blank, or the host is unreachable, the IT fails the build — it never reports
 * as skipped. See the recipe in {@code docs/template-v2-jdbc-sink-guide.md} for the full setup.
 *
 * <p>Default CI and local Maven runs skip {@code ChunkedPipelineDamengUpsertIT} without the flag.
 * The default CI bar for Dameng MERGE SQL generation remains the {@code JdbcSinkSqlBuilderTests}
 * unit tests, which require no live Dameng host.
 *
 * @author Gensokyo
 * @since 2026-07-28
 */
public final class DamengTestSupport {

    private DamengTestSupport() {
    }

    /**
     * @return {@code true} when operators opt in to real Dameng integration tests
     */
    public static boolean damengItEnabled() {
        String property = System.getProperty("dm.it");
        if (property != null && "true".equalsIgnoreCase(property.trim())) {
            return true;
        }
        String env = System.getenv("DG_DM_IT");
        return env != null && "true".equalsIgnoreCase(env.trim());
    }
}
