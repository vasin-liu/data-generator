/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sql;

/**
 * Classifies how a Template V2 SQL transform must be executed under chunked policy.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public enum ExecutionShape {

    /**
     * Row-local projection and filter over a single source; safe for chunked read/transform/write.
     */
    ROW_LOCAL,

    /**
     * Fact table chunked read with a small dimension materialized once (Task 9+).
     */
    BROADCAST_JOIN,

    /**
     * Requires full in-memory materialization (aggregates, joins without broadcast metadata, etc.).
     */
    MATERIALIZATION_REQUIRED
}
