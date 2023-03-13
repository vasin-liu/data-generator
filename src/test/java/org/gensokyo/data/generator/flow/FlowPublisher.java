/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.flow;

import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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
public class FlowPublisher<T> implements Flow.Publisher<T>, Flow.Subscription {
    private ThreadPoolTaskExecutor executor;
    private final BlockingQueue<T> queue;
    private final Supplier<T> supplier;
    private Flow.Subscriber<? super T> subscriber;
    private boolean completed = false;
    private boolean canceled = false;
    //已生产数量
    private final LongAdder produceQuantity = new LongAdder();
    //已消费数量
    private final LongAdder consumeQuantity = new LongAdder();
    //预计生产总数量
    private final long total;

    public FlowPublisher(@NonNull Supplier<T> supplier, long total) {
        this.supplier = supplier;
        this.total = total;
        this.queue = new ArrayBlockingQueue<>(100000);
    }

    public FlowPublisher(@NonNull Supplier<T> supplier, long total, int queueQuantity) {
        this.supplier = supplier;
        this.total = total;
        this.queue = new ArrayBlockingQueue<>(queueQuantity);
    }

    public FlowPublisher(@NonNull Supplier<T> supplier, long total, BlockingQueue<T> queue) {
        this.supplier = supplier;
        this.total = total;
        this.queue = Objects.requireNonNull(queue);
    }

    public FlowPublisher<T> parallel(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("并发数量不能小于1");
        }
        for (int i = 0; i < quantity; i++) {
            executor.execute(() -> {
                while (produceQuantity.sum() < total) {
                    if (queue.add(supplier.get())) {
                        produceQuantity.increment();
                    }
                }
            });
        }
        return this;
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
            T t = queue.poll();
            subscriber.onNext(t);
        }
    }

    @Override
    public void cancel() {
        canceled = true;
        //取消后下发onComplete消息
        if (!completed) {
            subscriber.onComplete();
        }
        //清除队列中的数据
        queue.clear();
    }
}
