/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import org.gensokyo.data.event.*;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.exception.NotEnoughElementException;
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
 * @since 2024/7/16 , Version 1.0.0
 */
public abstract class AbstractPipeline implements Pipeline, EventSource {
    private final List<Stage<?>> stages = new ArrayList<>();
    private final List<EventListener> listeners = new ArrayList<>();

    @Override
    public Pipeline next(Stage<?> stage) {
        if (Objects.nonNull(stage)) {
            stages.add(stage);
        }
        return this;
    }

    @Override
    public Value execute(Value input) {
        var result = input;
        if (CollectKit.isEmpty(stages)) {
            return result;
        }
        try {
            Value prev = input;
            for (Stage<?> stage : stages) {
                prev = stage.execute(prev);
            }
            result = prev;
        } catch (NotEnoughElementException e) {
            throw e;
        } catch (Exception e) {
            fireEvent(new ExceptionEvent(e));
            throw new DataGeneratorException(e);
        }
        fireEvent(new CompletionEvent(result));
        return result;
    }

    @Override
    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void fireEvent(Event event) {
        listeners.stream()
                .filter(Objects::nonNull)
                .filter(l -> l.support(event))
                .forEach(l -> l.handleEvent(event));
    }
}
