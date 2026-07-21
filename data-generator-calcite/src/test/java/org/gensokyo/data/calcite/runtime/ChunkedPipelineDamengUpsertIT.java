/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Optional Dameng MERGE upsert integration test (D-13, D-14).
 *
 * <p>Skipped by default; enable with {@code -Ddm.it=true} or {@code DG_DM_IT=true} when a Dameng
 * JDBC endpoint or container is available. MERGE SQL generation is covered by
 * {@code JdbcSinkSqlBuilderTests} without a live DM instance.
 *
 * @author Gensokyo
 * @since 2026-07-21
 */
@EnabledIf("org.gensokyo.data.calcite.support.DamengTestSupport#damengItEnabled")
class ChunkedPipelineDamengUpsertIT {

    /**
     * Placeholder for future real-DM upsert scenario when container wiring lands (D-14).
     */
    @Test
    void chunkedUpsertDamengMergeIsIdempotent() {
        // Real DM container wiring deferred; operators enable via DamengTestSupport when ready.
        Assumptions.abort("Dameng IT placeholder — wire DM Testcontainer when image available (D-14)");
    }
}
