/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.gensokyo.data.generator.constant.TaskStatus;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据生成任务对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/29 , Version 1.0.0
 */
@Data
@Entity
@Table(name = "task")
public class TaskPO implements Serializable {

    @Id
    private long id;
    private String name;
    private Date startTime = new Date();
    private Date endTime;
    private Long count = 0L;
    private TaskStatus status = TaskStatus.INIT;
    private String reason;
}
