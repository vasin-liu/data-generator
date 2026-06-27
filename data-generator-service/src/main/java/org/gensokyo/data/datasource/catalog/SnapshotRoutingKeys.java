/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.datasource.catalog;

/**
 * Isolated routing keys for snapshot-scoped JDBC pools so hot-reload cannot replace in-flight handles (D-10).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
final class SnapshotRoutingKeys {

    private SnapshotRoutingKeys() {
    }

    /**
     * @param instanceId active execution instance id
     * @param name       catalog connection name referenced by the template
     * @return routing key that is unique per execution
     */
    static String isolated(Long instanceId, String name) {
        return "snap:" + instanceId + ":" + name;
    }
}
