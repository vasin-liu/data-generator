/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;

import java.io.FileReader;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.StreamSupport;

/**
 * JSON迭代器实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/28 , Version 1.0.0
 */
public class JsonIterator<T extends JsonIteratorVO> extends AbstractIterator<T> {
    private final BlockingQueue<Value> queue = new LinkedBlockingQueue<>(10000);

    protected JsonIterator(IteratorContext<T> ctx) {
        super(ctx);
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.iterator(), "迭代器配置不能为空");
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
                                //不处理嵌套的数组
                                try {
                                    queue.put(SingleValue.of(node));
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
            } else {
                queue.put(SingleValue.of(jn));
            }
        } catch (Exception e) {
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

        throw new IllegalStateException("迭代器已经到达最大值");
    }
}
