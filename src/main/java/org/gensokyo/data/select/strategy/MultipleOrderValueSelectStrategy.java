/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.select.strategy;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.SelectStagePO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多次并且顺序选择
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class MultipleOrderValueSelectStrategy implements ValueSelectStrategy {
    @Override
    public Value select(final AtomicInteger index, final AtomicInteger selectedCount,
                        final SelectStagePO spo, final Value input) {
        var min = spo.getMinTimes();
        var max = spo.getMaxTimes();
        if (input.size() < min) {
            throw new DataGeneratorException(String.format("当前数据集的数据 %s 数量小于最小需要的数量 %s",
                    input.size(), min));
        }
        if (selectedCount.get() == 0) {
            //第一次，初始化一个给定范围内的随机数
            selectedCount.compareAndSet(0, RandomKit.nextInt(min, max));
        }
        var result = input;
        if (input instanceof ListValue lv) {
            if (index.getAndIncrement() < selectedCount.get()) {
                result = DatasetKit.extractValue(lv.get(0));
            } else {
                lv.remove(0);
            }
        }
        return result;
    }
}
