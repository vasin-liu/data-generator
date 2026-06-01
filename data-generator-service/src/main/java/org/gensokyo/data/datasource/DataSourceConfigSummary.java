/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import java.io.Serializable;
import java.time.Instant;

/**
 * API/UI view of a persisted datasource (password omitted).
 *
 * @param name            datasource key
 * @param url             JDBC URL
 * @param username            JDBC user
 * @param passwordSecretRef   optional secret reference instead of stored password
 * @param driverClassName     driver class
 * @param driverJarPath   optional uploaded driver path
 * @param enabled         active flag
 * @param createdAt       created timestamp
 * @param updatedAt       last update timestamp
 */
public record DataSourceConfigSummary(
        String name,
        String url,
        String username,
        String passwordSecretRef,
        String driverClassName,
        String driverJarPath,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) implements Serializable {
}
