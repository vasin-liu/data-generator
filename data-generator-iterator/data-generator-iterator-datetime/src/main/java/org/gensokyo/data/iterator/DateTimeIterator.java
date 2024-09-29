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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 时间迭代器实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
public class DateTimeIterator<T extends DateTimeIteratorVO> extends AbstractIterator<T> {

    private final AtomicReference<LocalDateTime> counter;
    private final LocalDateTime to;
    private final ChronoUnit unit;

    public DateTimeIterator(IteratorContext<T> ctx) {
        super(ctx);
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.iterator(), "迭代器配置不能为空");
        Assert.notNull(ctx.iterator().getFrom(), "时间迭代器配置的起始值不能为空");

        var it = ctx.iterator();
        var from = it.getFrom();
        var to = Objects.isNull(it.getTo()) ? LocalDateTime.now() : it.getTo();
        var step = it.getStep();
        this.unit = Objects.isNull(it.getUnit()) ? ChronoUnit.DAYS : it.getUnit();

        Assert.isTrue(from.atZone(ZoneId.of(Const.DEFAULT_ZONE_ID)).toInstant().isAfter(Instant.EPOCH),
                "时间迭代器配置的起始值必需大于'1970-01-01 00:00:00'");
        Assert.isTrue(from.isBefore(to), "迭代器配置的起始值必须小于或等于结束值");
        Assert.isTrue(step > 0 && step < Integer.MAX_VALUE, "时间迭代器配置的迭代间隔值必须大于0");
        this.counter = new AtomicReference<>(from);
        this.to = to;
    }

    @Override
    public boolean hasNext() {
        return counter.get().isBefore(to);
    }

    @Override
    public Value next() {
        var it = ctx.iterator();
        var prev = counter.get();
        if (prev.isBefore(to)) {
            var next = prev.plus(it.getStep(), this.unit);
            counter.compareAndSet(prev, next);
            return SingleValue.of(next);
        }

        throw new IllegalStateException("迭代器已经到达最大值");
    }
}
