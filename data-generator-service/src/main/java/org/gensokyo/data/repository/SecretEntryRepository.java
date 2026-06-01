/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.SecretEntryPO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA access for {@link SecretEntryPO}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public interface SecretEntryRepository extends JpaRepository<SecretEntryPO, String> {
}
