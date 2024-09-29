/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.selector.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.selector.value.ValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
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
public interface ValueSelectStrategy<S extends SelectStageVO, T extends ValueSelectStrategyVO> {

    /**
     * 数据选择策略
     *
     * @param index         选择索引
     * @param selectedCount 已选择选择次数
     * @param ctx           选择阶段上下文
     * @param vpo           选择阶段参数
     * @param input         给定数据集
     * @return 选择结果
     */
    Value select(final AtomicInteger index,
                 final AtomicInteger selectedCount,
                 StageContext<S> ctx,
                 T vpo,
                 final Value input);
}
