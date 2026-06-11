/*
 * Copyright 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import tools.jackson.databind.ObjectMapper;

import java.io.FileReader;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.StreamSupport;

public class JsonIterator<T extends JsonIteratorVO> extends AbstractIterator<T> {
    private final BlockingQueue<Value> queue = new LinkedBlockingQueue<>(10000);

    protected JsonIterator(IteratorContext<T> ctx) {
        super(ctx);
        Assert.notNull(ctx.template(), "Template must not be null");
        Assert.notNull(ctx.iterator(), "Json iterator config must not be null");
        var it = ctx.iterator();
        try {
            var om = new ObjectMapper();
            var jn = om.readTree(new FileReader(it.getPath()));
            if (jn.isArray()) {
                var startRow = Math.max(it.getStartRow(), 1);
                var endRow = it.getEndRow() < 1 ? Const.AMOUNT : it.getEndRow();
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(jn.iterator(), Spliterator.ORDERED), false)
                        .skip(startRow - 1)
                        .limit(endRow - startRow + 1)
                        .forEach(node -> {
                            if (Objects.nonNull(node)) {
                                try {
                                    queue.put(SingleValue.of(node));
                                }
                                catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
            }
            else {
                queue.put(SingleValue.of(jn));
            }
        }
        catch (Exception e) {
            throw new DataGeneratorException(e);
        }
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
        throw new IllegalStateException("No more JSON iterator values available");
    }
}
