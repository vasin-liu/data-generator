/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.ValueSelectStrategyType;

/**
 * 选择阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
public class SelectStagePO extends StagePO {

    /**
     * 选择策略
     */
    private ValueSelectStrategyType strategyType = ValueSelectStrategyType.REPEAT_RANDOM;

    /**
     * 选择数量
     */
    private int selectNum = 1;

    /**
     * 最少选择次数
     */
    private int minTimes = 1;

    /**
     * 最多选择次数
     */
    private int maxTimes = 1;
}
