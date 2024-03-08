/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.select.strategy.OnceOrderSelectStrategy;
import org.gensokyo.data.select.strategy.OnceRandomSelectStrategy;
import org.gensokyo.data.select.strategy.RepeatOrderSelectStrategy;
import org.gensokyo.data.select.strategy.RepeatRandomSelectStrategy;
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
    @ConditionalOnMissingBean(RepeatRandomSelectStrategy.class)
    public RepeatRandomSelectStrategy repeatRandomSelectStrategy() {
        return new RepeatRandomSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(OnceRandomSelectStrategy.class)
    public OnceRandomSelectStrategy onceRandomSelectStrategy() {
        return new OnceRandomSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(RepeatOrderSelectStrategy.class)
    public RepeatOrderSelectStrategy repeatOrderSelectStrategy() {
        return new RepeatOrderSelectStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(OnceOrderSelectStrategy.class)
    public OnceOrderSelectStrategy onceOrderSelectStrategy() {
        return new OnceOrderSelectStrategy();
    }
}
