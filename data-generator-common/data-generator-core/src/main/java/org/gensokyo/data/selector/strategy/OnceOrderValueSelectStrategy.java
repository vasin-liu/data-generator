/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.selector.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.selector.value.OnceOrderValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 只选一次并且顺序选择
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class OnceOrderValueSelectStrategy<S extends SelectStageVO, T extends OnceOrderValueSelectStrategyVO>
        implements ValueSelectStrategy<S, T>  {
    @Override
    public Value select(final AtomicInteger index,
                        final AtomicInteger selectedCount,
                        final StageContext<S> ctx,
                        final T vpo,
                        final Value input) {
        var num = vpo.getSelectNum() > 0 ? vpo.getSelectNum() : 1;
        if (input.size() < num) {
            throw new DataGeneratorException(String.format("当前数据集的数据 %s 数量小于需要的数量 %s",
                    input.size(), num));
        }
        var result = input;
        if (input instanceof ListValue lv) {
            var subList = lv.subList(0, num);
            result = DatasetKit.extractValue(ListValue.fromValueCollection(subList));
            lv.removeAll(subList);
        }
        return result;
    }
}
