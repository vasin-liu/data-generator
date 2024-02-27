/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.Context;
import org.gensokyo.data.po.WriterPO;
import org.gensokyo.data.stage.WriteStage;
import org.gensokyo.data.value.Value;
import org.gensokyo.data.write.WriterFactory;
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
    private final WriterFactory writerFactory;

    @Override
    public Value startup(final Context ctx) {
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.template().getTable(), "数据生成模板表配置不能为空");
        Assert.isTrue(CollectKit.isNotEmpty(ctx.template().getTable().getWriters()), "数据生成配置的表配置中必须至少配置一个写入器");
        var writers = ctx.template().getTable().getWriters();
        for (WriterPO wpo : writers) {
            write(wpo, ctx.dataset());
        }
        //无需返回数据集
        return null;
    }

    private void write(final WriterPO wpo, final Value dataset) {
        var pipeline = new DefaultWritePipeline();
        var writer = writerFactory.newInstance(wpo);
        var writeStage = new WriteStage(wpo, writer);
        pipeline.next(writeStage).execute(dataset);
    }

    @Override
    public void shutdown() {
        //nothing to do
    }
}
