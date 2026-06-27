/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

/**
 * Operator-visible health of a catalog entry after connectivity checks or hot-reload (D-26).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public enum ConnectionHealthStatus {

    /** Entry resolves and last connectivity check or reload succeeded. */
    HEALTHY,

    /**
     * Last reload or connectivity check failed; DB config is retained but runtime may serve
     * last known good handles until connectivity is restored (D-11).
     */
    DEGRADED
}
