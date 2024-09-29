/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker;

import org.gensokyo.data.script.vars.Variable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * 假数据生成配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/25 , Version 1.0.0
 */
@Configuration
public class DataFakerConfig {

    @Bean
    @ConditionalOnMissingBean
    public Variable dataFakerVariable(DataFaker dataFaker) {
        return DataFakerVariable.of(dataFaker);
    }

    @Bean
    @ConditionalOnMissingBean(DataFaker.class)
    public DataFaker dataFaker() {
        return new DataFaker(Locale.CHINA);
    }
}
