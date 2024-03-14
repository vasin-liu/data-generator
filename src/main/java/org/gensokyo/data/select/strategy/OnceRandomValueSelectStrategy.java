/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.select.strategy;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.stage.SelectStagePO;
import org.gensokyo.data.util.RandomKit;
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
public class OnceRandomValueSelectStrategy implements ValueSelectStrategy {
    @Override
    public Value select(final AtomicInteger index, final AtomicInteger selectedCount,
                        final SelectStagePO spo, final Value input) {
        var num = spo.getSelectNum();
        if (input.size() < num) {
            throw new DataGeneratorException(String.format("当前数据集的数据 %s 数量小于需要的数量 %s",
                    input.size(), num));
        }
        var result = RandomKit.choice(input, num);
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
