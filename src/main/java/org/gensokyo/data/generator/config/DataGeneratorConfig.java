/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.config;

import org.gensokyo.data.generator.cache.TemplateCache;
import org.gensokyo.data.generator.factory.ExecutorFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 数据生成配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@Configuration
public class DataGeneratorConfig {

    @Bean
    @ConditionalOnMissingBean(TemplateCache.class)
    public TemplateCache metaCache(DataGeneratorProperties props) {
        return new TemplateCache(props);
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
    @ConditionalOnMissingBean(ExecutorFactory.class)
    public ExecutorFactory executorFactory(AutowireCapableBeanFactory beanFactory) {
        return new ExecutorFactory(beanFactory);
    }
}
