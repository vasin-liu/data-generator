/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.collect.CollectKit;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 常量迭代器实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
public class ConstantIterator<T extends ConstantIteratorVO> extends AbstractIterator<T> {

    private final BlockingQueue<Value> queue;

    public ConstantIterator(IteratorContext<T> ctx) {
        super(ctx);
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.iterator(), "迭代器配置不能为空");
        var it = ctx.iterator();
        Assert.notNull(it.getDataset(), "常量迭代器配置的数据集不能为NULL");
        Assert.isTrue(CollectKit.isNotEmpty(it.getDataset()), "常量迭代器配置的数据集不能为空");
        this.queue = new LinkedBlockingQueue<>(it.getDataset().size());
        var ds = it.getDataset().stream().map(DatasetKit::toValue).toList();
        this.queue.addAll(ds);
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public Value next() {
        if (hasNext()) {
            return queue.poll();
        }

        throw new IllegalStateException("迭代器已经到达最大值");
    }
}
