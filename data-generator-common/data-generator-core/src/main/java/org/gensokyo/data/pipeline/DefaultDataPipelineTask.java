/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.GeneratorContext;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.generator.GeneratorFactory;
import org.gensokyo.data.util.DatetimeKit;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 默认的数据生成任务类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/14 , Version 1.0.0
 */
@Slf4j
public class DefaultDataPipelineTask implements Callable<Value> {
    private final TemplateContext ctx;

    @Setter(onMethod_ = @Autowired)
    private GeneratorFactory generatorFactory;

    protected DefaultDataPipelineTask(final TemplateContext ctx) {
        Assert.notNull(ctx, "数据生成上下文不能为空");
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.template().getGenerator(), "生成器配置不能为空");
        Assert.notNull(ctx.template().getIterator(), "生成器配置不能为空");
        this.ctx = ctx;
    }

    @Override
    public Value call() throws Exception {
        try {
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
        } catch (Exception e) {
            log.error("数据生成任务执行出现异常：", e);
        }
        return Value.EMPTY;
    }
}
