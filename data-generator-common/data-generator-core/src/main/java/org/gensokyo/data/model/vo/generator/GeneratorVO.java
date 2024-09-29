/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.generator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;

/**
 * 生成器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/19 , Version 1.0.0
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME
        , include = JsonTypeInfo.As.EXISTING_PROPERTY
        , property = "type"
        , visible = true
        // 反序列化时，如果没有匹配到子类，则使用默认实现类，即 AsyncGeneratorVO，
        // 除了 AsyncGeneratorVO，其他子类都需要在配置时指定type属性
        //, defaultImpl = AsyncGeneratorVO.class
)
public class GeneratorVO {

    /**
     * 生成器类型
     */
    private String type;

    /**
     * 批次大小
     */
    private int batchSize = Const.BATCH_SIZE;
}
