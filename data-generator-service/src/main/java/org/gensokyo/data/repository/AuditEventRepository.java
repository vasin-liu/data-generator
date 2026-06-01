/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.AuditEventPO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA access for {@link AuditEventPO}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public interface AuditEventRepository extends JpaRepository<AuditEventPO, Long> {
}
