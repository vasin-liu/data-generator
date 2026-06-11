/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.model.vo.iterator.IteratorVO;

/**
 * 迭代器抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
public abstract class AbstractIterator<T extends IteratorVO> implements Iterator<T> {

    protected final IteratorContext<T> ctx;

    protected AbstractIterator(IteratorContext<T> ctx) {
        this.ctx = ctx;
    }
}
