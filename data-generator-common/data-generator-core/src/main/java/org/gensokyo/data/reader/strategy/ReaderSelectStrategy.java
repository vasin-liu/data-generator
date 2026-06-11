/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.selector.reader.ReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;

/**
 * 读取器选择策略接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@FunctionalInterface
public interface ReaderSelectStrategy<S extends ReadStageVO, T extends ReaderVO, R extends ReaderSelectStrategyVO> {

    /**
     * 数据选择策略
     *
     * @param ctx 读取阶段上下文信息
     * @param rpo 读取器选择策略信息
     * @return 选择结果
     */
    T select(final StageContext<S> ctx, final R rpo);
}
