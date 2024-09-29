/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.converter;


import org.gensokyo.data.constant.Const;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 字符串转换器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class StringConverter implements Converter {
    @Override
    public Value convert(Value input) {
        if (Objects.isNull(input) || input.isNullOrEmpty()) {
            return SingleValue.of("");
        }
        if (input instanceof ListValue lv) {
            return SingleValue.of(
                    ((List<?>) lv.get()).stream().map(Object::toString).collect(Collectors.joining(Const.COMMA))
            );
        }
        if (input instanceof MapValue mv) {
            return SingleValue.of(mv.get());
        }
        return SingleValue.of(Objects.toString(input.get()));
    }
}
