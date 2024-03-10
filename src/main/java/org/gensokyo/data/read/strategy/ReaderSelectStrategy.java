/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read.strategy;

import org.gensokyo.data.po.ReadStagePO;

/**
 * 读取器选择策略接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@FunctionalInterface
public interface ReaderSelectStrategy {

    /**
     * 数据选择策略
     *
     * @param rpo 读取阶段信息
     * @return 选择结果
     */
    ReadStagePO.ReaderPO select(final ReadStagePO rpo);
}
