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
    };

    /**
     * 获取值
     *
     * @return 值
     */
    Object get();

    /**
     * 获取值类型
     *
     * @return 值类型
     */
    ValueType type();

    /**
     * 值是否为NULL或者为空
     *
     * @return 是否为NULL或者为空
     */
    boolean isNullOrEmpty();

    /**
     * 获取值长度大小
     *
     * @return 值长度大小
     */
    int size();
}
