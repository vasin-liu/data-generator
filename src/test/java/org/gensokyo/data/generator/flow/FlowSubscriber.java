/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.flow;

import org.springframework.lang.NonNull;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * 响应流数据消费者
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/31 , Version 1.0.0
 */
public class FlowSubscriber<T> implements Flow.Subscriber<T> {

    private final Consumer<T> consumer;
    private Flow.Subscription subscription;
    private boolean completed = false;
    private long requestCount;
    private static final long REQUEST_COUNT = 3;

    public FlowSubscriber(@NonNull Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        requestCount = REQUEST_COUNT;
        this.subscription = subscription;
        this.subscription.request(requestCount);
    }

    @Override
    public void onNext(T item) {
        requestCount--;
        consumer.accept(item);
        if (requestCount == 0 && !completed) {
            requestCount = REQUEST_COUNT;
            subscription.request(requestCount);
        }
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {
        requestCount = 0;
        completed = true;
    }
}