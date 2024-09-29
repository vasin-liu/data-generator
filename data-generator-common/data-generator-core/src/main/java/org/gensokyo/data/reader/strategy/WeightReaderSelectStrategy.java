/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader.strategy;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.selector.reader.WeightReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.kit.collect.CollectKit;

import java.security.SecureRandom;
import java.util.List;

/**
 * 带权重选择策略
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class WeightReaderSelectStrategy<S extends ReadStageVO, T extends ReaderVO, R extends WeightReaderSelectStrategyVO>
        implements ReaderSelectStrategy<S, T, R> {

    private static final SecureRandom RANDOM = new SecureRandom();

    @SuppressWarnings("unchecked")
    @Override
    public T select(final StageContext<S> ctx, final R spo) {
        S stage = ctx.stage();
        if (CollectKit.isEmpty(stage.getReaders())) {
            return null;
        }
        List<Integer> weights = stage.getReaders().stream().map(ReaderVO::getWeight).toList();
        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        int randomNum = RANDOM.nextInt(totalWeight) + 1;
        int cumulativeWeight = 0;
        int index = 0;

        while (cumulativeWeight < randomNum) {
            cumulativeWeight += weights.get(index);
            index++;
        }

        return (T) stage.getReaders().get(index - 1);
    }
}
