/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.event.OnDoneListener;
import org.gensokyo.data.event.OnExceptionListener;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.stage.Stage;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.collect.CollectKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 流水线抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public abstract class AbstractPipeline implements Pipeline {
    private final List<Stage> stages = new ArrayList<>();
    private OnDoneListener onDoneListener;
    private OnExceptionListener onExceptionListener;

    @Override
    public Pipeline next(Stage stage) {
        if (Objects.nonNull(stage)) {
            stages.add(stage);
        }
        return this;
    }

    @Override
    public Value execute(Value input) {
        var result = Value.EMPTY;
        if (CollectKit.isEmpty(stages)) {
            return result;
        }
        try {
            Value prev = input;
            for (Stage stage : stages) {
                prev = stage.execute(prev);
            }
            result = prev;
        } catch (Exception e) {
            if (Objects.nonNull(onExceptionListener)) {
                onExceptionListener.onException(e);
            } else {
                throw new DataGeneratorException(e);
            }
        }
        if (Objects.nonNull(onDoneListener)) {
            onDoneListener.onDone(result);
        }
        return result;
    }

    @Override
    public Pipeline onDone(OnDoneListener listener) {
        this.onDoneListener = listener;
        return this;
    }

    @Override
    public Pipeline onError(OnExceptionListener listener) {
        this.onExceptionListener = listener;
        return this;
    }
}
