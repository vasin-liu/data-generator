/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.DataSourceConfigPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistence for {@link DataSourceConfigPO}.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfigPO, String> {

    /**
     * @return enabled datasource rows for runtime bootstrap
     */
    List<DataSourceConfigPO> findByEnabledTrue();
}
