/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.function;

/**
 * 数据消费者
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/1 , Version 1.0.0
 */
@FunctionalInterface
public interface TripleConsumer<T1, T2, T3, R> {

    R accept(T1 t1, T2 t2, T3 t3);
}
