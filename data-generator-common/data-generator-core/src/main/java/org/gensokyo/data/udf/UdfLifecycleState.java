/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.udf;

/**
 * Lifecycle state for a versioned UDF registry entry.
 *
 * @author Gensokyo
 * @since 2026-06-17
 */
public enum UdfLifecycleState {

    /** Registered but not yet governed or resolvable at runtime. */
    DRAFT,

    /** Passed publish gate; eligible for runtime resolution. */
    PUBLISHED,

    /** Retired; must not resolve for new pipeline runs. */
    DEPRECATED
}
