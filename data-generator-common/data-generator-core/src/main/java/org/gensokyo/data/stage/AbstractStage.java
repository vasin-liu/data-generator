/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.event.*;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 阶段抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Slf4j
public abstract class AbstractStage<T extends StageVO> implements Stage<T>, EventSource {
    private final List<EventListener> listeners = new ArrayList<>();
    protected final StageContext<T> ctx;

    protected AbstractStage(StageContext<T> ctx) {
        this.ctx = ctx;
    }

    @Override
    public Value execute(Value input) {
        Value output;
        try {
            output = internalExecute(input);
        } catch (Exception e) {
            fireEvent(new ExceptionEvent(e));
            throw new DataGeneratorException(e);
        }
        fireEvent(new CompletionEvent(output));
        return output;
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
