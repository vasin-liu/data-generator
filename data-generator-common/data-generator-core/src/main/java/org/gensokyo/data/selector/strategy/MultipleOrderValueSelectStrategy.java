/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.selector.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.selector.value.MultipleOrderValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
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
public class MultipleOrderValueSelectStrategy<S extends SelectStageVO, T extends MultipleOrderValueSelectStrategyVO>
        implements ValueSelectStrategy<S, T> {
    @Override
    public Value select(final AtomicInteger index,
                        final AtomicInteger selectedCount,
                        final StageContext<S> ctx,
                        final T vpo,
                        final Value input) {
        var min = vpo.getMinTimes() > 0 ? vpo.getMinTimes() : 1;
        var max = vpo.getMaxTimes() > 0 ? vpo.getMaxTimes() : 1;
        var num = vpo.getSelectNum() > 0 ? vpo.getSelectNum() : 1;
        if (input.size() < num) {
            throw new DataGeneratorException(String.format("当前数据集的数据 %s 数量小于最小需要的数量 %s",
                    input.size(), num));
        }
        if (selectedCount.get() == 0) {
            //第一次，初始化一个给定范围内的随机数
            var count = min == max ? min : RandomKit.nextInt(min, max);
            selectedCount.compareAndSet(0, count);
        }

        var result = input;
        if (input instanceof ListValue lv) {
            var subList = lv.subList(0, num);
            if (index.getAndIncrement() < selectedCount.get()) {
                result = DatasetKit.extractValue(ListValue.fromValueCollection(subList));
            }
            if (index.get() >= selectedCount.get()) {
                lv.removeAll(subList);
            }
            resetIndex(index, selectedCount);
        }
        return result;
    }

    private void resetIndex(final AtomicInteger index, final AtomicInteger selectedCount) {
        if (index.get() >= selectedCount.get()) {
            //重置计数器
            index.compareAndSet(index.get(), 0);
            selectedCount.compareAndSet(selectedCount.get(), 0);
        }
    }
}
