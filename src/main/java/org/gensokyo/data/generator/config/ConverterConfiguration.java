/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.config;

import org.gensokyo.data.generator.converter.DateConverter;
import org.gensokyo.data.generator.converter.FloatConverter;
import org.gensokyo.data.generator.converter.LongConverter;
import org.gensokyo.data.generator.converter.StringConverter;
import org.gensokyo.data.generator.factory.ConverterFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 转换器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/12 , Version 1.0.0
 */
@Configuration
public class ConverterConfiguration {
    @Bean
    @ConditionalOnMissingBean(ConverterFactory.class)
    public ConverterFactory converterFactory(AutowireCapableBeanFactory beanFactory) {
        return new ConverterFactory(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean(DateConverter.class)
    public DateConverter dateFactory() {
        return new DateConverter();
    }

    @Bean
    @ConditionalOnMissingBean(FloatConverter.class)
    public FloatConverter floatConverter() {
        return new FloatConverter();
    }

    @Bean
    @ConditionalOnMissingBean(LongConverter.class)
    public LongConverter longConverter() {
        return new LongConverter();
    }

    @Bean
    @ConditionalOnMissingBean(StringConverter.class)
    public StringConverter stringConverter() {
        return new StringConverter();
    }
}
