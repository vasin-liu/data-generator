/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * 响应流数据发布器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/31 , Version 1.0.0
 */
@Slf4j
public class FlowPublisher<T> implements Flow.Publisher<T>, Flow.Subscription {
    private final Supplier<T> supplier;
    private Flow.Subscriber<? super T> subscriber;
    private volatile boolean completed = false;
    private volatile boolean canceled = false;
    //已消费数量
    private final LongAdder consumeQuantity = new LongAdder();
    //预计生产总数量
    private final long total;

    public FlowPublisher(@NonNull Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
        this.total = 10000;
    }

    public FlowPublisher(@NonNull Supplier<T> supplier, long total) {
        this.supplier = Objects.requireNonNull(supplier);
        if (total < 1) {
            throw new IllegalArgumentException("生成总数量不能小于1");
        }
        this.total = total;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        this.subscriber = Objects.requireNonNull(subscriber);
        this.subscriber.onSubscribe(this);
    }

    @Override
    public void request(long n) {
        //已完成或已取消，则不再下发消息
        if (completed || canceled) {
            return;
        }
        //数据消息下发完，则下发onComplete消息
        var cn = consumeQuantity.sum();
        if (cn == total) {
            completed = true;
            subscriber.onComplete();
            return;
        }
        //剩余数量
        var rn = total - cn;
        //下发数据消息
        for (var i = Math.min(n, rn); i > 0; i--) {
            T t = supplier.get();
            log.trace("Publisher => {}", JsonKit.write(t));
            subscriber.onNext(t);
            consumeQuantity.increment();
        }
    }

    @Override
    public void cancel() {
        canceled = true;
        //取消后下发onComplete消息
        if (!completed) {
            subscriber.onComplete();
        }
    }
}
