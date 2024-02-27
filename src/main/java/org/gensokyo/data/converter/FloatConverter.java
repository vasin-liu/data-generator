/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.converter;

import org.springframework.core.convert.converter.Converter;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 浮点转换器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/9 , Version 1.0.0
 */
public class FloatConverter implements Converter<Object, Float> {

    @Override
    public Float convert(@Nullable Object source) {
        if (Objects.isNull(source)) {
            return null;
        }
        return Float.valueOf(source.toString());
    }
}
