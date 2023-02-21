/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import org.gensokyo.data.generator.listener.GeneratorListener;

import java.util.concurrent.Callable;

/**
 * 生成器接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public interface Generator<V> extends Callable<V> {

    void registerListener(GeneratorListener listener);
}
