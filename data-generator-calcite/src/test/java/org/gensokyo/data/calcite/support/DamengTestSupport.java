/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.support;

/**
 * Gate for optional Dameng Testcontainers integration tests (D-14).
 *
 * <p>Primary Dameng upsert proof remains {@code JdbcSinkSqlBuilderTests} MERGE SQL unit tests.
 * Enable a real DM container IT when an image or host is available:
 *
 * <ul>
 *   <li>JVM: {@code -Ddm.it=true}</li>
 *   <li>Environment: {@code DG_DM_IT=true}</li>
 * </ul>
 *
 * <p>Default CI and local Maven runs skip {@code ChunkedPipelineDamengUpsertIT} without the flag.
 *
 * @author Gensokyo
 * @since 2026-07-21
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
