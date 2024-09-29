/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.value.Value;

/**
 * 迭代器接口定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
public interface Iterator<T extends IteratorVO> extends AutoCloseable {

    /**
     * 是否有下一个元素
     *
     * @return {@code true} 有下一个元素，{@code false} 无下一个元素
     */
    boolean hasNext();

    /**
     * 下一个元素值
     * 假如已经达到可获取的最大值，则抛出 {@link IllegalStateException} 异常
     *
     * @return 返回值对象 {@link Value}
     */
    Value next();

    @Override
    default void close() throws Exception {

    }
}
