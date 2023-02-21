/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.config;

import org.gensokyo.data.generator.factory.ScriptFactory;
import org.gensokyo.data.generator.faker.DataFaker;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * 脚本配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/12 , Version 1.0.0
 */
@Configuration
public class ScriptConfiguration {

    @Bean
    @ConditionalOnMissingBean(ScriptFactory.class)
    public ScriptFactory scriptFactory(AutowireCapableBeanFactory beanFactory, DataFaker dataFaker) {
        return new ScriptFactory(beanFactory, dataFaker);
    }

    @Bean
    @ConditionalOnMissingBean(DataFaker.class)
    public DataFaker dataFaker() {
        return new DataFaker(Locale.CHINA);
    }
}
