/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.GeneratorContext;
import org.gensokyo.data.value.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步生成器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/19 , Version 1.0.0
 */
@Slf4j
public class AsyncGenerator<G extends AsyncGeneratorVO> extends AbstractGenerator<G> {

    private MdcTaskDecorator mdcTaskDecorator;


    @Autowired
    public void setMdcTaskDecorator(MdcTaskDecorator mdcTaskDecorator) {
        this.mdcTaskDecorator = mdcTaskDecorator;
    }

    public AsyncGenerator(final GeneratorContext<G> ctx) {
        super(ctx);
    }

    @Override
    protected void doJob(Value input) {
        producerExecutor.submit(() -> produce(input));
    }

    @Override
    protected ThreadPoolTaskExecutor producerExecutor() {
        var p = ctx.generator().getExecutor();
        var executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(mdcTaskDecorator);
        //核心线程池大小
        executor.setCorePoolSize(p.getCoreSize());
        //最大线程数
        executor.setMaxPoolSize(p.getMaxSize());
        //队列容量
        executor.setQueueCapacity(p.getQueueCapacity());
        //活跃时间
        executor.setKeepAliveSeconds(p.getKeepAliveSeconds());
        //线程名字前缀
        var namePrefix = String.format("DG-PRODUCER-%s-%s-%s-",
                ctx.template().getId(), ctx.template().getName(), ctx.template().getInstanceId());
        executor.setThreadNamePrefix(namePrefix);
        // 设置线程池关闭的时候等待所有任务都完成再继续销毁其他的Bean
        executor.setWaitForTasksToCompleteOnShutdown(p.isWaitForJobsToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(p.getAwaitTerminationSeconds());
        //队列满时阻塞主线程提交任务动作
        executor.setRejectedExecutionHandler(new BlockWhenQueueFullHandler());
        executor.initialize();
        return executor;
    }
}
