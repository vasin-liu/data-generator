/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.select.strategy;

import org.gensokyo.data.po.stage.SelectStagePO;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.Value;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重复并且随机选择
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
public class RepeatRandomValueSelectStrategy implements ValueSelectStrategy {
    @Override
    public Value select(final AtomicInteger index, final AtomicInteger selectedCount,
                        final SelectStagePO spo, final Value input) {
        var num = spo.getSelectNum() > 0 ? spo.getSelectNum() : 1;
        return RandomKit.choice(input, num);
    }
}
