/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.select.strategy;

import org.gensokyo.data.po.SelectStagePO;
import org.gensokyo.data.value.Value;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 选择策略接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@FunctionalInterface
public interface ValueSelectStrategy {

    /**
     * 数据选择策略
     *
     * @param index         选择索引
     * @param selectedCount 已选择选择次数
     * @param spo           选择阶段参数
     * @param input         给定数据集
     * @return 选择结果
     */
    Value select(final AtomicInteger index, final AtomicInteger selectedCount, SelectStagePO spo, final Value input);
}
