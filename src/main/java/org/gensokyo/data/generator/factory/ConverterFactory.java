/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.convert.converter.Converter;

/**
 * 转换器工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ConverterFactory implements Factory {

    private final AutowireCapableBeanFactory beanFactory;

    public Converter<Object, ?> getBean(Class<? extends Converter<Object, ?>> clazz) {
        return beanFactory.getBean(clazz);
    }
}
