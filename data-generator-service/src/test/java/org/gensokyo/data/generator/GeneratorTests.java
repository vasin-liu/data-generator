/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.iterator.NumberIterator;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.value.Value;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 生成器测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/31 , Version 1.0.0
 */
class GeneratorTests {

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final LongAdder total = new LongAdder();

    @Test
    void case1() {
        doIteration(itvo());
        System.out.printf("已生成 %s 条数据%n", total.sum());
    }

    NumberIteratorVO itvo() {
        var p = new NumberIteratorVO();
        p.setFrom(1);
        p.setTo(1);
        p.setStep(1);
        var c = new NumberIteratorVO();
        c.setFrom(1);
        c.setTo(1);
        c.setStep(1);
        p.setIterator(c);
        return p;
    }

    void doIteration(IteratorVO ivo, Value... parentValues) {
        var ctx = new IteratorContext<>(new TemplateVO(), (NumberIteratorVO) ivo);
        var it = new NumberIterator<>(ctx);
        while (it.hasNext()) {
            var currentValue = it.next();
            var finalValues = new Value[parentValues.length + 1];
            System.arraycopy(parentValues, 0, finalValues, 0, parentValues.length);
            finalValues[parentValues.length] = currentValue;
            if (Objects.nonNull(ivo.getIterator())) {
                doIteration(ivo.getIterator(), finalValues);
            } else {
                if (!initialized.get()) {
                    total.increment();
                    System.out.println("预加载数据：" + currentValue.get());
                    initialized.compareAndSet(false, true);
                } else {
                    total.increment();
                    System.out.println("生成数据：" + currentValue.get());
                }
            }
        }


    }
}
