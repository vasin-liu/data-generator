/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read.strategy;

import org.gensokyo.data.po.ReadStagePO;
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
public class WeightReaderSelectStrategy implements ReaderSelectStrategy {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 数据选择策略
     *
     * @param rpo 读取阶段信息
     * @return 选择结果
     */
    @Override
    public ReadStagePO.ReaderPO select(final ReadStagePO rpo) {
        if (CollectKit.isEmpty(rpo.getReaders())) {
            return null;
        }
        List<Integer> weights = rpo.getReaders().stream().map(ReadStagePO.ReaderPO::getWeight).toList();
        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        int randomNum = RANDOM.nextInt(totalWeight) + 1;
        int cumulativeWeight = 0;
        int index = 0;

        while (cumulativeWeight < randomNum) {
            cumulativeWeight += weights.get(index);
            index++;
        }

        return rpo.getReaders().get(index - 1);
    }
}
