/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;

import java.util.Collection;
import java.util.Objects;

/**
 * 数据选取阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class SelectStage implements Stage {
    @Override
    public Value execute(Value input) {
        if (Objects.isNull(input) || input.isNullOrEmpty()) {
            return Value.EMPTY;
        }
        var value = input;
        if (input instanceof ListValue lv) {
            value = RandomKit.choiceOne(lv);
        }
        //部分数据可能为Supplier类型，需要执行Supplier获取真实数据
        Object r = value.get();
        if (r instanceof Collection<?> c) {
            return SingleValue.of(RandomKit.choiceOne(c));
        } else {
            return SingleValue.of(r);
        }
    }
}
