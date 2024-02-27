/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.event.OnDoneListener;
import org.gensokyo.data.event.OnExceptionListener;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.stage.StagePO;
import org.gensokyo.data.value.Value;

import java.util.Objects;

/**
 * 阶段抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Slf4j
public abstract class AbstractStage<T extends StagePO> implements Stage {
    private OnDoneListener onDoneListener;
    private OnExceptionListener onExceptionListener;

    protected final StageContext<T> ctx;

    protected AbstractStage(StageContext<T> ctx) {
        this.ctx = ctx;
    }

    @Override
    public Value execute(Value input) {
        Value output = Value.EMPTY;
        try {
            output = internalExecute(input);
            if (Objects.nonNull(onDoneListener)) {
                onDoneListener.onDone(output);
            }
        } catch (Exception e) {
            if (Objects.nonNull(onExceptionListener)) {
                onExceptionListener.onException(e);
            } else {
                throw new DataGeneratorException(e);
            }
        }
        return output;
    }

    @Override
    public Stage onDone(OnDoneListener listener) {
        this.onDoneListener = listener;
        return this;
    }

    @Override
    public Stage onError(OnExceptionListener listener) {
        this.onExceptionListener = listener;
        return this;
    }
}
