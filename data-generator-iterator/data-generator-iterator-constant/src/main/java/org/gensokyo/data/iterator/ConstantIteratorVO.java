/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.iterator.IteratorVO;

import java.util.List;

/**
 * 常量迭代器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(IteratorVO.class)
@JsonSubType(value = "CONSTANT")
public class ConstantIteratorVO extends IteratorVO {

    /**
     * 重复迭代次数：默认值为：1
     */
    private int repeat = 1;

    private List<Object> dataset;
}
