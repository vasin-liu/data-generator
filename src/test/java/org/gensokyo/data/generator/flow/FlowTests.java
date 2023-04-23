/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.flow;

import org.gensokyo.data.generator.FlowPublisher;
import org.gensokyo.data.generator.FlowSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

/**
 * 响应流测试类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/3/13 , Version 1.0.0
 */
class FlowTests {

    @Test
    void case1() {
        var la = new LongAdder();
        var rnd = new Random();
        var pub = new FlowPublisher<>(() -> rnd.nextInt(1000), 10);
        var sub = new FlowSubscriber<Integer>(i -> {
            System.out.println("value => " + i);
            la.increment();
        }, 2);
        pub.subscribe(sub);
        Assertions.assertEquals(10, la.sum());
    }
}
