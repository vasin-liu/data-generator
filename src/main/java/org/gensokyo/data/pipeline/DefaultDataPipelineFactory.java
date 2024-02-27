/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.Context;
import org.gensokyo.data.util.DatetimeKit;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * 线程池流水线工厂实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/24 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDataPipelineFactory implements PipelineFactory {
    private final DefaultReadPipelineFactory defaultReadPipelineFactory;
    private final DefaultWritePipelineFactory defaultWritePipelineFactory;
    private final DefaultRowPipelineFactory defaultRowPipelineFactory;
    private final ThreadPoolTaskExecutor executor;

    @Override
    public Value startup(final Context ctx) {
        Assert.notNull(ctx, "数据生成上下文不能为空");
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        var template = ctx.template();
        //从数据源中读取，该数据集的数据内容为 MapValue Map<String,Value> 即 Map<字段名, 数据集>
        var dataset = defaultReadPipelineFactory.startup(ctx);
        //分批次生成数据
        doBatch(template.getBatchSize(), template.getAmount(), new Context(template, dataset));
        //无需返回数据
        return null;
    }

    private void doBatch(int pageSize, int total, final Context ctx) {
        var stopWatch = new StopWatch();
        stopWatch.start();
        int pages = (total + pageSize - 1) / pageSize;
        for (int i = 1; i <= pages; i++) {
            int size = pageSize;
            if (i == pages) {
                size = total - (i - 1) * pageSize;
            }
            //执行当前批次任务
            doJob(i, size, ctx);
        }
        stopWatch.stop();
        log.info("当前批量任务执行完成，总计耗时：{} ", DatetimeKit.humanized(stopWatch.getTotalTimeMillis()));
    }

    private void doJob(int index, int size, final Context ctx) {
        final List<Value> data = new CopyOnWriteArrayList<>();
        for (int i = 0; i < size; i++) {
            try {
                List<CompletableFuture<Value>> futures = new ArrayList<>();
                //生成一行数据
                final int ii = i;
                var rowCtx = new Context(ctx.template(), ctx.dataset());
                CompletableFuture<Value> future = supplyAsync(() -> defaultRowPipelineFactory.startup(rowCtx), executor)
                        .whenComplete((r, e) -> {
                            //r 类型为 MapValue Map<String,Value> 即 Map<字段名, 数据集>
                            if (Objects.isNull(e)) {
                                data.add(r);
                            } else {
                                log.error(String.format("分批次生成数据出现异常，当前第 %s 页，第 %s 条数据，异常信息：", index, ii), e);
                            }
                        });
                futures.add(future);
                allOf(futures.toArray(new CompletableFuture[]{})).join();
            } catch (Exception e) {
                log.error(String.format("分批次生成数据出现异常，当前第 %s 页，每页 %s 条数据，异常信息：", index, size), e);
            }
        }
        //写入数据
        defaultWritePipelineFactory.startup(new Context(ctx.template(), ListValue.fromValueList(data)));
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        defaultReadPipelineFactory.shutdown();
        defaultWritePipelineFactory.shutdown();
    }
}
