/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataCache;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.exception.NotEnoughElementException;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final DefaultWritePipelineFactory defaultWritePipelineFactory;
    private final DefaultRowPipelineFactory defaultRowPipelineFactory;
    private final ThreadPoolTaskExecutor executor;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public Value startup(final TemplateContext ctx) {
        Assert.notNull(ctx, "数据生成上下文不能为空");
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        var template = ctx.template();
        //分批次生成数据
        doBatch(template.getBatchSize(), template.getAmount(), ctx);
        //无需返回数据
        return Value.EMPTY;
    }

    @Override
    public void cleanup(final TemplateContext ctx) {
        initialized.compareAndSet(true, false);
        DataCache.remove(ctx.template().getName());
        defaultRowPipelineFactory.cleanup(ctx);
        defaultWritePipelineFactory.cleanup(ctx);
    }

    private void doBatch(int pageSize, int total, final TemplateContext ctx) {
        var stopWatch = new StopWatch();
        stopWatch.start();
        int pages = (total + pageSize - 1) / pageSize;
        for (int i = 1; i <= pages; i++) {
            int size = pageSize;
            if (i == pages) {
                size = total - (i - 1) * pageSize;
            }
            //执行当前批次任务
            if (ctx.template().getGlobal().isAsync()) {
                doAsyncJob(i, size, ctx);
            } else {
                doSyncJob(i, size, ctx);
            }
        }
        stopWatch.stop();
        log.info("当前批量任务执行完成，总计耗时：{} ", DatetimeKit.humanized(stopWatch.getTotalTimeMillis()));
    }

    private void doAsyncJob(int index, int size, final TemplateContext ctx) {
        final List<Value> data = new CopyOnWriteArrayList<>();
        try {
            List<CompletableFuture<Value>> futures = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                //生成一行数据
                final int ii = i;
                var rowCtx = new TemplateContext(ctx.template(), ctx.dataset());
                if (initialized.get()) {
                    CompletableFuture<Value> future = supplyAsync(() -> defaultRowPipelineFactory.startup(rowCtx), executor)
                            .whenComplete((r, e) -> {
                                //r 类型为 MapValue Map<String,Value> 即 Map<字段名, 数据集>
                                if (Objects.isNull(e)) {
                                    data.add(r);
                                } else {
                                    checkException(index, ii, e);
                                }
                            });
                    futures.add(future);
                } else {
                    //第一次执行，先加载缓存数据
                    data.add(loadDataToMemory(rowCtx));
                }
            }
            allOf(futures.toArray(new CompletableFuture[]{})).get();
            //写入数据
            defaultWritePipelineFactory.startup(new TemplateContext(ctx.template(), ListValue.fromValueCollection(data)));
        } catch (NotEnoughElementException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(String.format("分批次生成数据出现异常，当前第 %s 页，每页 %s 条数据，异常信息：", index, size), e);
        }
    }

    private void doSyncJob(int index, int size, final TemplateContext ctx) {
        final List<Value> data = new CopyOnWriteArrayList<>();
        try {
            for (int i = 0; i < size; i++) {
                var rowCtx = new TemplateContext(ctx.template(), ctx.dataset());
                var r = defaultRowPipelineFactory.startup(rowCtx);
                data.add(r);
            }
            //写入数据
            defaultWritePipelineFactory.startup(new TemplateContext(ctx.template(), ListValue.fromValueCollection(data)));
        } catch (NotEnoughElementException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(String.format("分批次生成数据出现异常，当前第 %s 页，每页 %s 条数据，异常信息：", index, size), e);
        }
    }


    private void checkException(int pageIndex, int rowIndex, Throwable e) {
        if (e instanceof NotEnoughElementException ne) {
            throw ne;
        } else {
            throw new DataGeneratorException(String.format("分批次生成数据出现异常，当前第 %s 页，第 %s 条数据，异常信息：", pageIndex, rowIndex), e);
        }
    }

    private Value loadDataToMemory(final TemplateContext ctx) {
        //先执行一行记录生成缓存数据
        var result = Value.EMPTY;
        var tdc = DataCache.getOrCreate(ctx.template().getName());
        if (tdc.isEmpty()) {
            var rowCtx = new TemplateContext(ctx.template(), ctx.dataset());
            result = defaultRowPipelineFactory.startup(rowCtx);
            //初始化完成
            initialized.compareAndSet(false, true);
            log.info("模板 {} 所需的缓存数据已加载完毕", ctx.template().getName());
        }
        return result;
    }

    @Override
    public void shutdown(final TemplateContext ctx) {
        this.cleanup(ctx);
        executor.shutdown();
        defaultRowPipelineFactory.shutdown(ctx);
        defaultWritePipelineFactory.shutdown(ctx);
    }
}
