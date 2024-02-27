/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.converter;

import org.apache.logging.log4j.util.Strings;
import org.springframework.core.convert.converter.Converter;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 字符串转换器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/6 , Version 1.0.0
 */
public class StringConverter implements Converter<Object, String> {

    @Override
    public String convert(@Nullable Object source) {
        if (Objects.isNull(source)) {
            return Strings.EMPTY;
        }
        return source.toString();
    }
}
