/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.value;

import org.gensokyo.data.constant.ValueType;
import org.gensokyo.kit.collect.MapKit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map值对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/27 , Version 1.0.0
 */
public class MapValue extends ConcurrentHashMap<String, Value> implements Value {

    public MapValue() {
        super();
    }

    public MapValue(int initialCapacity) {
        super(initialCapacity);
    }

    public MapValue(Map<String, ? extends Value> map) {
        super(map);
    }

    @Override
    public Object get() {
        if (isNullOrEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new HashMap<>();
        for (Entry<String, Value> entry : entrySet()) {
            if (Objects.nonNull(entry.getKey()) && Objects.nonNull(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue().get());
            }
        }
        return result;
    }

    @Override
    public ValueType type() {
        return ValueType.MAP;
    }

    @Override
    public boolean isNullOrEmpty() {
        return isEmpty();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Value fromMap(Map<String, ?> map) {
        if (MapKit.isEmpty(map)) {
            return EMPTY;
        }
        var result = new MapValue();
        map.forEach((k, v) -> {
            if (v instanceof Value v1) {
                result.put(k, v1);
            } else if (v instanceof Collection<?> c) {
                result.put(k, ListValue.fromObjectCollection(List.copyOf(c)));
            } else if (v instanceof Map m) {
                result.put(k, fromMap(m));
            } else {
                result.put(k, SingleValue.of(v));
            }
        });
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "value=\"Ignore Map Type\"" +
                ", calculatedValue=\"Ignore Map Type\"" +
                ", type=" + type() +
                ", isNullOrEmpty=" + isNullOrEmpty() +
                ", size=" + size() +
                "}";
    }
}
