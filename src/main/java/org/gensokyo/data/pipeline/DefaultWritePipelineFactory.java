/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.po.WriteStagePO;
import org.gensokyo.data.stage.StageFactory;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.collect.CollectKit;

/**
 * 默认数据写入流水线工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultWritePipelineFactory implements PipelineFactory {
    private final StageFactory stageFactory;

    @Override
    public Value startup(final TemplateContext ctx) {
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.template().getTable(), "数据生成模板表配置不能为空");
        Assert.isTrue(CollectKit.isNotEmpty(ctx.template().getTable().getWriters()), "数据生成配置的表配置中必须至少配置一个写入器");
        var writers = ctx.template().getTable().getWriters();
        for (WriteStagePO wpo : writers) {
            var stageCtx = new StageContext<>(ctx.template(), null, wpo);
            write(stageCtx, ctx.dataset());
        }
        //无需返回数据集
        return null;
    }

    private void write(final StageContext<WriteStagePO> ctx, final Value dataset) {
        var pipeline = new DefaultWritePipeline();
        var stage = stageFactory.newInstance(ctx);
        pipeline.next(stage).execute(dataset);
    }

    @Override
    public void shutdown() {
        //nothing to do
    }
}
