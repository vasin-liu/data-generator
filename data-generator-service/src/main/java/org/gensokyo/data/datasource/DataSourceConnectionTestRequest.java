/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

import java.io.Serializable;

/**
 * Request body for JDBC connection test without persisting.
 *
 * @param url             JDBC URL
 * @param username        user
 * @param password        password
 * @param driverClassName driver class
 * @param driverJarPath   optional stored jar path
 */
public record DataSourceConnectionTestRequest(
        String url,
        String username,
        String password,
        String driverClassName,
        String driverJarPath) implements Serializable {
}
