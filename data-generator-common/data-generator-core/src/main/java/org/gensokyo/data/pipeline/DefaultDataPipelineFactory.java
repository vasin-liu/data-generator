/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataSet;
import org.gensokyo.data.context.GeneratorContext;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.generator.GeneratorFactory;
import org.gensokyo.data.util.DatetimeKit;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.springframework.util.StopWatch;

import java.util.Objects;

/**
 * 线程池流水线工厂实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/24 , Version 1.0.0
 */
@Deprecated
@Slf4j
@RequiredArgsConstructor
public class DefaultDataPipelineFactory implements PipelineFactory {
    private final GeneratorFactory generatorFactory;

    @Override
    public Value startup(final TemplateContext ctx) {
        Assert.notNull(ctx, "数据生成上下文不能为空");
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.template().getGenerator(), "生成器配置不能为空");
        Assert.notNull(ctx.template().getIterator(), "生成器配置不能为空");
        var stopWatch = new StopWatch();
        stopWatch.start();
        //设置实例编号
        if (Objects.isNull(ctx.template().getInstanceId())) {
            ctx.template().setInstanceId(RandomKit.snowFlake().nextId());
        }
        var nctx = new GeneratorContext<>(ctx.template(), ctx.template().getGenerator());
        var generator = generatorFactory.newInstance(nctx);
        generator.startup();
        stopWatch.stop();
        log.info("当前批量任务执行完成，总计耗时：{} ", DatetimeKit.humanized(stopWatch.getTotalTimeMillis()));
        //无需返回数据
        return Value.EMPTY;
    }

    @Override
    public void cleanup(final TemplateContext ctx) {
        DataSet.remove(ctx.template().getId(), ctx.template().getInstanceId());
    }

    @Override
    public void shutdown(final TemplateContext ctx) {
        this.cleanup(ctx);
    }
}
