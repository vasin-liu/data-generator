/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.selector.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.selector.value.RepeatOrderValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重复并且顺序选择
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class RepeatOrderValueSelectStrategy<S extends SelectStageVO, T extends RepeatOrderValueSelectStrategyVO>
        implements ValueSelectStrategy<S, T> {
    @Override
    public Value select(final AtomicInteger index,
                        final AtomicInteger selectedCount,
                        final StageContext<S> ctx,
                        final T vpo,
                        final Value input) {
        var num = vpo.getSelectNum();
        var size = input.size();
        resetIndex(index, size);
        var result = input;
        if (input instanceof ListValue lv) {
            List<Value> list = new ArrayList<>(num);
            for (int i = 0; i < num; i++) {
                resetIndex(index, size);
                var val = lv.get(index.getAndIncrement());
                list.add(val);
            }
            result = ListValue.fromValueCollection(list);
        }
        return result;
    }

    private void resetIndex(AtomicInteger index, int size) {
        if (index.get() >= size) {
            // 重置索引
            index.compareAndSet(index.get(), 0);
        }
    }
}
