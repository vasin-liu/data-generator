/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import org.gensokyo.data.constant.ValueType;
import org.gensokyo.data.value.Value;

/**
 * 终止值定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/30 , Version 1.0.0
 */
public class EndValue implements Value {

    @Override
    public Object get() {
        return null;
    }

    @Override
    public ValueType type() {
        return ValueType.EMPTY;
    }

    @Override
    public boolean isNullOrEmpty() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return "{" +
                "value=null" +
                ", calculatedValue=" + get() +
                ", type=" + type() +
                ", isNullOrEmpty=" + isNullOrEmpty() +
                ", size=" + size() +
                "}";
    }
}
