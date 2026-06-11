/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.iterator.IteratorVO;

/**
 * JSON文件迭代器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/28 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(IteratorVO.class)
@JsonSubType(value = "JSON")
public class JsonIteratorVO extends IteratorVO {
    /**
     * 来源路径
     */
    private String path;
    /**
     * 开始行
     */
    private int startRow = 1;
    /**
     * 结束行
     */
    private int endRow = Const.AMOUNT;
}
