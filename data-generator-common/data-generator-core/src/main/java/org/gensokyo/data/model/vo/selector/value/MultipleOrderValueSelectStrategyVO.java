/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.selector.value;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;

/**
 * 多次顺序选择策略配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/10 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(ValueSelectStrategyVO.class)
@JsonSubType(value = Const.ValueSelectStrategyType.MULTIPLE_ORDER)
public class MultipleOrderValueSelectStrategyVO extends ValueSelectStrategyVO {
    /**
     * 最少选择次数
     */
    private int minTimes = 1;

    /**
     * 最多选择次数
     */
    private int maxTimes = 1;
}
