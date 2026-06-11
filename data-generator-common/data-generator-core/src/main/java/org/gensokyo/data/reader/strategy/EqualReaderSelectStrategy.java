/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.selector.reader.EqualReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.kit.collect.CollectKit;

/**
 * 平等选择策略
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class EqualReaderSelectStrategy<S extends ReadStageVO, T extends ReaderVO, R extends EqualReaderSelectStrategyVO>
        implements ReaderSelectStrategy<S, T, R> {

    @SuppressWarnings("unchecked")
    @Override
    public T select(final StageContext<S> ctx, final R spo) {
        S stage = ctx.stage();
        if (CollectKit.isEmpty(stage.getReaders())) {
            return null;
        }
        return (T) RandomKit.choiceOne(stage.getReaders());
    }
}
