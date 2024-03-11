/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.convert;


import com.google.common.base.Joiner;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.json.JsonKit;

import java.util.List;
import java.util.Objects;

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
            return SingleValue.of(Joiner.on(Const.COMMA).join((List<?>) lv.get()));
        }
        if (input instanceof MapValue mv) {
            return SingleValue.of(mv.get());
        }
        return SingleValue.of(Objects.toString(input.get()));
    }
}
