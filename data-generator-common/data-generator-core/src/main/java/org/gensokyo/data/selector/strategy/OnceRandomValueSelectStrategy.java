/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.selector.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.exception.NotEnoughElementException;
import org.gensokyo.data.model.vo.selector.value.OnceRandomValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.util.RandomKitExt;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 只选一次并且随机选择
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class OnceRandomValueSelectStrategy<S extends SelectStageVO, T extends OnceRandomValueSelectStrategyVO>
        implements ValueSelectStrategy<S, T> {
    @Override
    public Value select(final AtomicInteger index,
                        final AtomicInteger selectedCount,
                        final StageContext<S> ctx,
                        final T spo,
                        final Value input) {
        var num = spo.getSelectNum() > 0 ? spo.getSelectNum() : 1;
        if (input.size() < num) {
            throw new NotEnoughElementException(String.format("当前数据集的数据 %s 数量小于需要的数量 %s",
                    input.size(), num));
        }
        var result = RandomKitExt.choice(input, num);
        if (input instanceof ListValue lv) {
            if (result instanceof ListValue l) {
                lv.removeAll(l);
            } else {
                lv.remove(result);
            }
        }
        return result;
    }
}
