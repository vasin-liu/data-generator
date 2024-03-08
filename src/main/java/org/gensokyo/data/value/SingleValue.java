/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.value;

import org.gensokyo.data.constant.ValueType;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 单值对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/27 , Version 1.0.0
 */
public class SingleValue implements Value {

    private final Object value;

    private SingleValue(Object value) {
        this.value = value;
    }

    public static SingleValue of(Object value) {
        return new SingleValue(value);
    }

    @Override
    public Object get() {
        if (isNullOrEmpty()) {
            return null;
        }

        if (value instanceof Supplier<?> s) {
            return s.get();
        } else {
            return value;
        }
    }

    @Override
    public ValueType type() {
        return ValueType.SINGLE;
    }

    @Override
    public boolean isNullOrEmpty() {
        return Objects.isNull(value);
    }

    @Override
    public int size() {
        return 1;
    }
}
