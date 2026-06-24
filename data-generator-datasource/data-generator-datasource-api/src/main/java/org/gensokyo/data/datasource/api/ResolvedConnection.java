/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.api;

/**
 * Runtime handle returned by {@link ConnectionCatalog#resolve(String, ConnectionKind)}.
 * Kind-specific permits carry adapter-owned opaque handles without framework dependencies in this module.
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-24
 */
public sealed interface ResolvedConnection permits JdbcResolvedConnection, KafkaResolvedConnection, ElasticsearchResolvedConnection {

    /**
     * @return resolved connection name
     */
    String connectionName();

    /**
     * @return resolved connection kind
     */
    ConnectionKind kind();
}
