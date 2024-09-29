/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.GeneratorContext;
import org.gensokyo.data.value.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 同步生成器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/19 , Version 1.0.0
 */
@Slf4j
public class SyncGenerator<T extends SyncGeneratorVO> extends AbstractGenerator<T> {

    public SyncGenerator(final GeneratorContext<T> ctx) {
        super(ctx);
    }

    @Override
    protected void doJob(final Value input) {
        produce(input);
    }

    @Override
    protected ThreadPoolTaskExecutor producerExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        //核心线程池大小
        executor.setCorePoolSize(1);
        //最大线程数
        executor.setMaxPoolSize(1);
        //队列容量
        executor.setQueueCapacity(1);
        //活跃时间
        executor.setKeepAliveSeconds(240);
        //线程名字前缀
        executor.setThreadNamePrefix("DG-PRODUCER-" + ctx.template().getId() + "-" + ctx.template().getName() + "-");
        // 设置线程池关闭的时候等待所有任务都完成再继续销毁其他的Bean
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(Integer.MAX_VALUE);
        //队列满时阻塞主线程提交任务动作
        executor.setRejectedExecutionHandler(new BlockWhenQueueFullHandler());
        executor.initialize();
        return executor;
    }
}
