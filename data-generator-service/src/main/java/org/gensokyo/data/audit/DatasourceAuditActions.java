/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.audit;

import java.util.Set;

/**
 * Datasource lifecycle audit action codes (D-22).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public final class DatasourceAuditActions {

    /** New managed connection persisted. */
    public static final String CREATE = "DATASOURCE_CREATE";
    /** Existing managed connection updated. */
    public static final String UPDATE = "DATASOURCE_UPDATE";
    /** Managed connection removed/disabled. */
    public static final String DELETE = "DATASOURCE_DELETE";
    /** Hot-reload attempt after save (success or failure). */
    public static final String RELOAD = "DATASOURCE_RELOAD";
    /** Connection entered DEGRADED health state. */
    public static final String DEGRADED = "DATASOURCE_DEGRADED";
    /** Connectivity test failed. */
    public static final String CONNECTIVITY_FAIL = "DATASOURCE_CONNECTIVITY_FAIL";
    /** Governance policy blocked save/publish/run. */
    public static final String GOVERNANCE_BLOCK = "DATASOURCE_GOVERNANCE_BLOCK";

    /** Console audit category filter value for all datasource events (D-25). */
    public static final String CATEGORY = "DATASOURCE";

    /** All datasource action codes for category filtering. */
    public static final Set<String> ALL_ACTIONS = Set.of(
            CREATE, UPDATE, DELETE, RELOAD, DEGRADED, CONNECTIVITY_FAIL, GOVERNANCE_BLOCK);

    private DatasourceAuditActions() {
    }
}
