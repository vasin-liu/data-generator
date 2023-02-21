/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.factory;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.generator.domain.TemplatePO;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
@RequiredArgsConstructor
public class ExecutorFactory implements Factory {

    private final AutowireCapableBeanFactory beanFactory;

    public ThreadPoolTaskExecutor newInstance(TemplatePO template) {
        var executor = new ThreadPoolTaskExecutor();
        //配置核心线程数
        executor.setCorePoolSize(template.getGlobal().getExecutor().getCoreSize());
        //配置最大线程数
        executor.setMaxPoolSize(template.getGlobal().getExecutor().getMaxSize());
        //配置队列大小
        executor.setQueueCapacity(template.getGlobal().getExecutor().getQueueCapacity());
        //配置线程池中的线程的名称前缀
        executor.setThreadNamePrefix(String.format("DG-%s-", template.getName()));
        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }
}
