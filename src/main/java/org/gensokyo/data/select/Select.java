/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.select;

import org.gensokyo.data.select.strategy.SelectStrategy;
import org.gensokyo.data.value.Value;

/**
 * 选择值接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/26 , Version 1.0.0
 */
@FunctionalInterface
public interface Select {

    /**
     * 选择值
     *
     * @param strategy 选择策略
     * @param num      选择数量
     * @return 选择的Value值
     */
    Value select(SelectStrategy strategy, int num);
}
