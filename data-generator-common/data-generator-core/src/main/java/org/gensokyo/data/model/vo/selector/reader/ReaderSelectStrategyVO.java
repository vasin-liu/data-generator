/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.selector.reader;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;

/**
 * 选择器配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/10 , Version 1.0.0
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME
        , include = JsonTypeInfo.As.EXISTING_PROPERTY
        , property = "type"
        , visible = true
        // 反序列化时，如果没有匹配到子类，则使用默认实现类
        , defaultImpl = EqualReaderSelectStrategyVO.class
)
public class ReaderSelectStrategyVO {

    /**
     * 读取器选择策略，默认使用等值选择策略
     */
    private String type = Const.ReaderSelectStrategyType.EQUAL;
}
