/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.reader.ReaderVO;

/**
 * SPEL表达式数据读取配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(ReaderVO.class)
@JsonSubType(value = "SPEL")
public class SpelReaderVO extends ReaderVO {
    /**
     * 表达式
     */
    private String content;
    /**
     * 表达式执行次数
     */
    private int times = 1;
}
