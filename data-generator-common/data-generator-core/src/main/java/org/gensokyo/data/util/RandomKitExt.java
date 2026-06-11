/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.util;

import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;

import java.util.Objects;

/**
 * 随机工具扩展
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/25 , Version 1.0.0
 */
public class RandomKitExt extends RandomKit {

    public static Value choiceOne(Value value) {
        return choice(value, 1);
    }

    public static Value choice(Value value, int num) {
        if (Objects.isNull(value) || value.isNullOrEmpty()) {
            return null;
        }
        if (num < 1) {
            throw new DataGeneratorException("选择元素数量不能小于1");
        }
        if (value instanceof ListValue lv) {
            if (num == 1) {
                int idx = RANDOM.nextInt(lv.size());
                return lv.get(idx);
            } else {
                var nlv = new ListValue();
                //数量大于数据集合的长度时，返回全部数据？
                for (int i = 0; i < num; ++i) {
                    int idx = RANDOM.nextInt(lv.size());
                    nlv.add(lv.get(idx));
                }
                return nlv;
            }
        }
        return value;
    }
}
