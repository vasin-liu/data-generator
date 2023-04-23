/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * 响应流数据消费者
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/31 , Version 1.0.0
 */
@Slf4j
public class FlowSubscriber<T> implements Flow.Subscriber<T> {

    private ThreadPoolTaskExecutor executor;
    private final Consumer<T> consumer;
    private Flow.Subscription subscription;
    private boolean completed = false;
    private long requestCount;
    private final long quantity;

    public FlowSubscriber(@NonNull Consumer<T> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
        this.quantity = 10;
    }

    public FlowSubscriber(@NonNull Consumer<T> consumer, long requestCount) {
        this.consumer = Objects.requireNonNull(consumer);
        if (requestCount < 1) {
            throw new IllegalArgumentException("订阅数量不能小于1");
        }
        this.quantity = requestCount;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        requestCount = quantity;
        this.subscription = subscription;
        this.subscription.request(requestCount);
    }

    @Override
    public void onNext(T item) {
        requestCount--;
        log.trace("Subscriber => {}", JsonKit.write(item));
        consumer.accept(item);
        if (requestCount == 0 && !completed) {
            requestCount = quantity;
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