/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.SelectStrategyType;

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
    private SelectStrategyType strategyType = SelectStrategyType.REPEAT_RANDOM;

    /**
     * 选择数量
     */
    private int num = 1;
}
