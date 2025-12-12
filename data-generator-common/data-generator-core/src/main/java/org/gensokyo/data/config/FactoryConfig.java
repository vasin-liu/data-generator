/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.generator.GeneratorFactory;
import org.gensokyo.data.iterator.IteratorFactory;
import org.gensokyo.data.pipeline.DefaultDataPipelineFactory;
import org.gensokyo.data.pipeline.DefaultDataPipelineTaskFactory;
import org.gensokyo.data.pipeline.DefaultRowPipelineFactory;
import org.gensokyo.data.pipeline.DefaultWritePipelineFactory;
import org.gensokyo.data.reader.ReaderFactory;
import org.gensokyo.data.reader.strategy.ReaderSelectStrategyFactory;
import org.gensokyo.data.selector.strategy.ValueSelectStrategyFactory;
import org.gensokyo.data.stage.StageFactory;
import org.gensokyo.data.writer.WriterFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
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
public class FactoryConfig {

    @Bean
    @ConditionalOnMissingBean(ReaderFactory.class)
    public ReaderFactory readerFactory(ApplicationContext ctx) {
        return new ReaderFactory(ctx);
    }

    @Bean
    @ConditionalOnMissingBean(WriterFactory.class)
    public WriterFactory writerFactory(ApplicationContext ctx) {
        return new WriterFactory(ctx);
    }

    @Bean
    @ConditionalOnMissingBean(StageFactory.class)
    public StageFactory stageFactory(ApplicationContext ctx) {
        return new StageFactory(ctx);
    }

    @Bean
    @ConditionalOnMissingBean(IteratorFactory.class)
    public IteratorFactory factorFactory(ApplicationContext ctx) {
        return new IteratorFactory(ctx);
    }

    @Bean
    @ConditionalOnMissingBean(ValueSelectStrategyFactory.class)
    public ValueSelectStrategyFactory valueSelectStrategyFactory(ApplicationContext ctx) {
        return new ValueSelectStrategyFactory(ctx);
    }

    @Bean
    @ConditionalOnMissingBean(ReaderSelectStrategyFactory.class)
    public ReaderSelectStrategyFactory readerSelectStrategyFactory(ApplicationContext ctx) {
        return new ReaderSelectStrategyFactory(ctx);
    }

    @Bean
    @ConditionalOnMissingBean(GeneratorFactory.class)
    public GeneratorFactory generatorFactory(ApplicationContext ctx) {
        return new GeneratorFactory(ctx);
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
    public DefaultDataPipelineFactory defaultDataPipelineFactory(GeneratorFactory generatorFactory) {
        return new DefaultDataPipelineFactory(generatorFactory);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultDataPipelineTaskFactory.class)
    public DefaultDataPipelineTaskFactory defaultDataPipelineTaskFactory(ApplicationContext ctx) {
        return new DefaultDataPipelineTaskFactory(ctx);
    }
}
