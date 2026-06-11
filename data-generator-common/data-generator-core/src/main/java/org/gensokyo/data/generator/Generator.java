/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator;

import org.gensokyo.data.model.vo.generator.GeneratorVO;

/**
 * 数据生成器接口定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/19 , Version 1.0.0
 */
public interface Generator<G extends GeneratorVO> {

    /**
     * 启动生成器
     */
    void startup();

    /**
     * 清理生成器
     */
    void cleanup();

    /**
     * 关闭生成器
     */
    void shutdown();
}
