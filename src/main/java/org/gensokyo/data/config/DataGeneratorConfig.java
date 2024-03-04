/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.config;

import org.gensokyo.data.cache.ConfigCache;
import org.gensokyo.data.faker.DataFaker;
import org.gensokyo.data.pipeline.DefaultDataPipelineFactory;
import org.gensokyo.data.pipeline.DefaultReadPipelineFactory;
import org.gensokyo.data.pipeline.DefaultRowPipelineFactory;
import org.gensokyo.data.pipeline.DefaultWritePipelineFactory;
import org.gensokyo.data.read.ReaderFactory;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.stage.StageFactory;
import org.gensokyo.data.write.WriterFactory;
import org.gensokyo.data.yaml.JacksonParser;
import org.gensokyo.data.yaml.YamlParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Configuration
public class DataGeneratorConfig {

    @Bean
    @ConditionalOnMissingBean(JacksonParser.class)
    public JacksonParser jacksonParser() {
        return new JacksonParser();
    }

    @Bean
    @ConditionalOnMissingBean(ConfigCache.class)
    public ConfigCache configCache(DataGeneratorProperties props, YamlParser yamlParser) {
        return new ConfigCache(props, yamlParser);
    }

    @Bean
    @ConditionalOnMissingBean(DataFaker.class)
    public DataFaker dataFaker() {
        return new DataFaker(Locale.CHINA);
    }

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
    @ConditionalOnMissingBean(DefaultReadPipelineFactory.class)
    public DefaultReadPipelineFactory defaultReadPipelineFactory(StageFactory stageFactory) {
        return new DefaultReadPipelineFactory(stageFactory);
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

    @Bean(name = "dataGeneratorTaskExecutor")
    public ThreadPoolTaskExecutor dataGeneratorTaskExecutor(DataGeneratorProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //配置核心线程数
        executor.setCorePoolSize(props.getCorePoolSize());
        //配置最大线程数
        executor.setMaxPoolSize(props.getMaxPoolSize());
        //配置队列大小
        executor.setQueueCapacity(props.getQueueCapacity());
        //配置线程池中的线程的名称前缀
        executor.setThreadNamePrefix("DG-TASK-");
        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
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
