/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.converter;

import org.gensokyo.data.value.Value;

/**
 * 转换器接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@FunctionalInterface
public interface Converter {

    /**
     * 将输入值转换为指定的输出值
     *
     * @param input 输入值
     * @return 输出值
     */
    Value convert(Value input);
}
