/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.kit.time.Patterns;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 时间迭代器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(IteratorVO.class)
@JsonSubType(value = "DATETIME")
public class DateTimeIteratorVO extends IteratorVO {

    /**
     * 1970-01-01T00:00:00Z
     */
    @JsonFormat(pattern = Patterns.DATETIME_STANDARD, timezone = "Asia/Shanghai")
    private LocalDateTime from;

    @JsonFormat(pattern = Patterns.DATETIME_STANDARD, timezone = "Asia/Shanghai")
    private LocalDateTime to = LocalDateTime.now();

    private int step = 1;

    private ChronoUnit unit = ChronoUnit.DAYS;
}
