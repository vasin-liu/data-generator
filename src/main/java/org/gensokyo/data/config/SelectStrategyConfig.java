/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.read.strategy.EqualReaderSelectStrategy;
import org.gensokyo.data.read.strategy.WeightReaderSelectStrategy;
import org.gensokyo.data.select.strategy.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class SelectStrategyConfig {

    @Bean
    @ConditionalOnMissingBean(RepeatRandomValueSelectStrategy.class)
    public RepeatRandomValueSelectStrategy repeatRandomSelectStrategy() {
        return new RepeatRandomValueSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(OnceRandomValueSelectStrategy.class)
    public OnceRandomValueSelectStrategy onceRandomSelectStrategy() {
        return new OnceRandomValueSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(RepeatOrderValueSelectStrategy.class)
    public RepeatOrderValueSelectStrategy repeatOrderSelectStrategy() {
        return new RepeatOrderValueSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(MultipleOrderValueSelectStrategy.class)
    public MultipleOrderValueSelectStrategy multipleOrderValueSelectStrategy() {
        return new MultipleOrderValueSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(OnceOrderValueSelectStrategy.class)
    public OnceOrderValueSelectStrategy onceOrderSelectStrategy() {
        return new OnceOrderValueSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(EqualReaderSelectStrategy.class)
    public EqualReaderSelectStrategy equalSelectStrategy() {
        return new EqualReaderSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(WeightReaderSelectStrategy.class)
    public WeightReaderSelectStrategy weightSelectStrategy() {
        return new WeightReaderSelectStrategy();
    }

}
