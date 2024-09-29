/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.selector.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.selector.value.RepeatRandomValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.util.RandomKitExt;
import org.gensokyo.data.value.Value;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重复并且随机选择
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class RepeatRandomValueSelectStrategy<S extends SelectStageVO, T extends RepeatRandomValueSelectStrategyVO>
        implements ValueSelectStrategy<S, T> {
    @Override
    public Value select(final AtomicInteger index,
                        final AtomicInteger selectedCount,
                        final StageContext<S> ctx,
                        final T vpo, final Value input) {
        var num = vpo.getSelectNum() > 0 ? vpo.getSelectNum() : 1;
        return RandomKitExt.choice(input, num);
    }
}
