/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.parser;

import org.springframework.core.convert.support.DefaultConversionService;

/**
 * 消息转换解析抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/11 , Version 1.0.0
 */
public abstract class AbstractConversionServiceOutputParser<T> implements OutputParser<T> {

    private final DefaultConversionService conversionService;

    public AbstractConversionServiceOutputParser(DefaultConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public DefaultConversionService getConversionService() {
        return conversionService;
    }

}
