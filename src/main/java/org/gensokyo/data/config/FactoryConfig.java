/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.pipeline.DefaultDataPipelineFactory;
import org.gensokyo.data.pipeline.DefaultRowPipelineFactory;
import org.gensokyo.data.pipeline.DefaultWritePipelineFactory;
import org.gensokyo.data.read.ReaderFactory;
import org.gensokyo.data.read.strategy.ReaderSelectStrategyFactory;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.select.strategy.ValueSelectStrategyFactory;
import org.gensokyo.data.stage.StageFactory;
import org.gensokyo.data.write.WriterFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class FactoryConfig {

    @Bean
    @ConditionalOnMissingBean(ScriptFactory.class)
    public ScriptFactory scriptFactory(AutowireCapableBeanFactory beanFactory) {
        return new ScriptFactory(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean(ReaderFactory.class)
    public ReaderFactory readerFactory(AutowireCapableBeanFactory beanFactory) {
        return new ReaderFactory(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean(WriterFactory.class)
    public WriterFactory writerFactory(AutowireCapableBeanFactory beanFactory) {
        return new WriterFactory(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean(StageFactory.class)
    public StageFactory stageFactory(AutowireCapableBeanFactory beanFactory) {
        return new StageFactory(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean(ValueSelectStrategyFactory.class)
    public ValueSelectStrategyFactory valueSelectStrategyFactory(AutowireCapableBeanFactory beanFactory) {
        return new ValueSelectStrategyFactory(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean(ReaderSelectStrategyFactory.class)
    public ReaderSelectStrategyFactory readerSelectStrategyFactory(AutowireCapableBeanFactory beanFactory) {
        return new ReaderSelectStrategyFactory(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultWritePipelineFactory.class)
    public DefaultWritePipelineFactory defaultWritePipelineFactory(StageFactory stageFactory) {
        return new DefaultWritePipelineFactory(stageFactory);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultRowPipelineFactory.class)
    public DefaultRowPipelineFactory defaultRowPipelineFactory(StageFactory stageFactory) {
        return new DefaultRowPipelineFactory(stageFactory);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultDataPipelineFactory.class)
    public DefaultDataPipelineFactory defaultDataPipelineFactory(
            DefaultWritePipelineFactory defaultWritePipelineFactory,
            DefaultRowPipelineFactory defaultRowPipelineFactory,
            @Qualifier(value = "dataGeneratorTaskExecutor")
            ThreadPoolTaskExecutor threadPoolTaskExecutor
    ) {
        return new DefaultDataPipelineFactory(defaultWritePipelineFactory,
                defaultRowPipelineFactory, threadPoolTaskExecutor);
    }
}
