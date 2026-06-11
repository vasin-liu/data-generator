/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.selector;

import org.gensokyo.data.model.vo.selector.value.ValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.selector.strategy.ValueSelectStrategy;
import org.gensokyo.data.value.Value;

/**
 * 选择值接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@FunctionalInterface
public interface Selector<S extends SelectStageVO, T extends ValueSelectStrategyVO> {

    /**
     * 选择值
     *
     * @param strategy 选择策略
     * @param ctx      选择阶段上下文
     * @param vpo      选择阶段信息
     * @return 选择的Value值
     */
    Value select(final ValueSelectStrategy<S, T> strategy, final S ctx, final T vpo);
}
