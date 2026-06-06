/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.MessagingClusterConfigPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA access for console-managed messaging clusters.
 *
 * @author Gensokyo
 * @since 2026-06-04
 */
public interface MessagingClusterConfigRepository extends JpaRepository<MessagingClusterConfigPO, String> {

    /**
     * @param clusterType {@link org.gensokyo.data.messaging.MessagingClusterType} name
     * @return rows of the given type
     */
    List<MessagingClusterConfigPO> findByClusterType(String clusterType);
}
