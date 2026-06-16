/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.TaskExecutionPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@link TaskExecutionPO}.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionPO, Long> {

    /**
     * @param instanceId run instance id
     * @return row when present
     */
    Optional<TaskExecutionPO> findByInstanceId(Long instanceId);

    /**
     * @param templateId template id
     * @return executions newest first
     */
    List<TaskExecutionPO> findByTemplateIdOrderByFinishedAtDesc(Long templateId);

    /**
     * @param templateId template id
     * @param statuses   active statuses
     * @return whether any matching row exists
     */
    boolean existsByTemplateIdAndStatusIn(Long templateId, Collection<String> statuses);

    /**
     * @param status execution status name
     * @return matching rows newest first
     */
    List<TaskExecutionPO> findByStatusOrderByFinishedAtDesc(String status);
}
