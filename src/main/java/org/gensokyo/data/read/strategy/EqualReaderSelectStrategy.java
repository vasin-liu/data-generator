/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read.strategy;

import org.gensokyo.data.po.ReadStagePO;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.kit.collect.CollectKit;

/**
 * 平等选择策略
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class EqualReaderSelectStrategy implements ReaderSelectStrategy {

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
        return RandomKit.choiceOne(rpo.getReaders());
    }
}
