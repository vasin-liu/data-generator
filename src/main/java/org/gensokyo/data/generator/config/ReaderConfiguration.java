/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.config;

import org.gensokyo.data.generator.factory.ReaderFactory;
import org.gensokyo.data.generator.faker.DataFaker;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 读取器配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/12 , Version 1.0.0
 */
@Configuration
public class ReaderConfiguration {

    @Bean
    @ConditionalOnMissingBean(ReaderFactory.class)
    public ReaderFactory readerFactory(AutowireCapableBeanFactory beanFactory, DataFaker dataFaker) {
        return new ReaderFactory(beanFactory, dataFaker);
    }
}
