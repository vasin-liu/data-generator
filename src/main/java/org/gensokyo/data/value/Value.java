/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.value;

import org.gensokyo.data.constant.ValueType;

/**
 * 值对象接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/27 , Version 1.0.0
 */
public interface Value {
    Value EMPTY = new Value() {
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
    };

    Object get();

    ValueType type();

    boolean isNullOrEmpty();
}
