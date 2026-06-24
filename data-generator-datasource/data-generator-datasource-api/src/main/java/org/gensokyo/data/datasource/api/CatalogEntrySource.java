/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

/**
 * Origin of a catalog entry in the merged bootstrap + managed view (D-06, D-26).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public enum CatalogEntrySource {

    /** Read-only entry registered from application YAML at startup (D-24). */
    BOOTSTRAP,

    /** Operator-managed entry persisted via console CRUD (D-23). */
    MANAGED
}
