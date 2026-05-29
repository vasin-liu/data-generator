/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource;

/**
 * Result of loading a JDBC driver in an isolated or application class loader.
 *
 * @param driverClassName resolved driver class
 * @param classLoader     loader that can instantiate the driver (never null)
 * @param bundled         true when loaded from shipped {@code jdbc-bundled/} jars
 * @author Gensokyo
 * @since 2026-05-29
 */
public record JdbcDriverLoadResult(String driverClassName, ClassLoader classLoader, boolean bundled) {
}
