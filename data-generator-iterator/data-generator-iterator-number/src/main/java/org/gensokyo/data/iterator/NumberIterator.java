/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 数字迭代器实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
public class NumberIterator<T extends NumberIteratorVO> extends AbstractIterator<T> {

    private final AtomicLong counter;

    public NumberIterator(IteratorContext<T> ctx) {
        super(ctx);
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.iterator(), "迭代器配置不能为空");
        var it = ctx.iterator();
        var from = it.getFrom();
        var to = it.getTo();
        var step = it.getStep();
        Assert.isTrue(from > 0 && from < Long.MAX_VALUE, "数字迭代器配置的起始值必须大于0");
        Assert.isTrue((to > 0 && to < Long.MAX_VALUE) || Const.NEGATIVE_ONE == ctx.iterator().getTo(),
                "数字迭代器配置的结束值必须大于0或者等于-1");
        Assert.isTrue(from <= to, "数字迭代器配置的起始值必须小于或等于结束值");
        Assert.isTrue(step > 0 && step < Integer.MAX_VALUE, "数字迭代器配置的迭代间隔值必须大于0");
        this.counter = new AtomicLong(ctx.iterator().getFrom());
    }

    @Override
    public boolean hasNext() {
        var it = ctx.iterator();
        if (it.getTo() == Const.NEGATIVE_ONE) {
            counter.get();
            return true;
        }
        return counter.get() <= it.getTo();
    }

    @Override
    public Value next() {
        var it = ctx.iterator();
        var to = it.getTo();
        if ((to == Const.NEGATIVE_ONE)) {
            return SingleValue.of(counter.getAndAdd(it.getStep()));
        }

        if (counter.get() <= to) {
            return SingleValue.of(counter.getAndAdd(it.getStep()));
        }

        throw new IllegalStateException("迭代器已经到达最大值");
    }
}
