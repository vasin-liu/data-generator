/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.flow;

import org.springframework.lang.NonNull;

import java.util.Spliterator;
import java.util.concurrent.Flow;

/**
 * 响应流数据发布器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/31 , Version 1.0.0
 */
public class FlowPublisher<T> implements Flow.Publisher<T>, Flow.Subscription {
    private final Spliterator<T> spliterator;
    private Flow.Subscriber<? super T> subscriber;
    private boolean completed = false;
    private boolean canceled = false;

    public FlowPublisher(@NonNull Spliterator<T> spliterator) {
        this.spliterator = spliterator;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        this.subscriber.onSubscribe(this);
    }

    @Override
    public void request(long n) {
        //已完成或已取消，则不再下发消息
        if (completed || canceled) {
            return;
        }
        //数据消息下发完，则下发onComplete消息
        long size = spliterator.getExactSizeIfKnown();
        if (size == 0) {
            completed = true;
            subscriber.onComplete();
            return;
        }
        //下发数据消息
        for (long i = Math.min(n, size); i > 0; i--) {
            spliterator.tryAdvance(subscriber::onNext);
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
