/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.repository;

import org.gensokyo.data.generator.domain.TaskPO;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 任务仓储类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/8 , Version 1.0.0
 */
@Repository
public interface TaskRepository extends CrudRepository<TaskPO, Long> {

}