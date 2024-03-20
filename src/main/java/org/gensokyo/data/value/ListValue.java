/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.value;

import org.gensokyo.data.constant.ValueType;
import org.gensokyo.data.po.stage.SelectStagePO;
import org.gensokyo.data.select.Selector;
import org.gensokyo.data.select.strategy.ValueSelectStrategy;
import org.gensokyo.kit.collect.CollectKit;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 集合值
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/27 , Version 1.0.0
 */
public class ListValue extends CopyOnWriteArrayList<Value> implements Value, Selector {
    /**
     * 当前选择的索引下标
     */
    private final AtomicInteger index = new AtomicInteger(0);
    /**
     * 已选择的元素次数
     */
    private final AtomicInteger selectedCount = new AtomicInteger(0);

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public ListValue() {
        super();
    }


    public ListValue(Collection<? extends Value> coll) {
        super(coll);
    }

    @Override
    public Object get() {
        if (isNullOrEmpty()) {
            return List.of();
        }
        return this.stream().map(Value::get).toList();
    }

    @Override
    public ValueType type() {
        return ValueType.LIST;
    }

    @Override
    public boolean isNullOrEmpty() {
        return isEmpty();
    }

    public Value first() {
        return isEmpty() ? EMPTY : get(0);
    }

    public ListValue addValue(Value value) {
        if (Objects.isNull(value) || value.isNullOrEmpty()) {
            return this;
        }
        if (value instanceof ListValue lv) {
            this.addAll(lv);
        } else {
            this.add(value);
        }
        return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Value fromObjectCollection(Collection<Object> values) {
        if (CollectKit.isEmpty(values)) {
            return EMPTY;
        }
        var result = new ListValue();
        values.forEach(v -> {
            if (v instanceof Value v1) {
                result.add(v1);
            } else if (v instanceof Collection<?> c) {
                result.add(fromObjectCollection(List.copyOf(c)));
            } else if (v instanceof Map m) {
                result.add(MapValue.fromMap(m));
            } else {
                result.add(SingleValue.of(v));
            }
        });
        return result;
    }

    public static Value fromValueCollection(Collection<Value> values) {
        if (CollectKit.isEmpty(values)) {
            return EMPTY;
        }
        return new ListValue(values);
    }

    public static Value fromMapCollection(Collection<Map<String, Object>> values) {
        if (CollectKit.isEmpty(values)) {
            return EMPTY;
        }
        var result = new ListValue();
        values.forEach(v -> result.add(MapValue.fromMap(v)));
        return result;
    }

    @Override
    public Value select(ValueSelectStrategy strategy, SelectStagePO spo) {
        try {
            rwLock.writeLock().lock();
            if (Objects.isNull(strategy)) {
                return this;
            }
            return strategy.select(index, selectedCount, spo, this);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListValue values)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        if (values.index.get() != index.get()) {
            return false;
        }
        return rwLock.equals(values.rwLock);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + index.hashCode();
        result = 31 * result + rwLock.hashCode();
        return result;
    }
}
