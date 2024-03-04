/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.strategy;

import org.gensokyo.data.value.Value;

/**
 * 选择策略接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@FunctionalInterface
public interface SelectStrategy {

    /**
     * 数据选择策略
     *
     * @param input 给定数据集
     * @return 选择结果
     */
    Value select(Value input);
}
