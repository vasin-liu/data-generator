/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.repository;

import org.gensokyo.data.model.po.TaskSchedulePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Persistence for cron-driven template run schedules.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
public interface TaskScheduleRepository extends JpaRepository<TaskSchedulePO, Long> {

    /**
     * @param templateId template id
     * @return schedules for the template
     */
    List<TaskSchedulePO> findByTemplateIdOrderByCreatedAtDesc(Long templateId);

    /**
     * Finds enabled schedules that are due for triggering.
     *
     * @param now current timestamp
     * @return due schedule rows
     */
    @Query("""
            select s
            from TaskSchedulePO s
            where s.enabled = true
              and s.nextTriggerAt is not null
              and s.nextTriggerAt <= :now
            order by s.nextTriggerAt asc
            """)
    List<TaskSchedulePO> findDue(@Param("now") Instant now);
}
