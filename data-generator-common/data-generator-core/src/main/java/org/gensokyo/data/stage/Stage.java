/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.value.Value;

/**
 * 处理阶段接口定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/1/19 , Version 1.0.0
 */
@FunctionalInterface
public interface Stage<T extends StageVO> {
    /**
     * 执行处理阶段（供外部调用方法）
     *
     * @param input 输入值
     * @return 输出值
     */
    default Value execute(Value input) {
        return internalExecute(input);
    }

    /**
     * 执行处理阶段（实际内部处理方法）
     *
     * @param input 输入值
     * @return 输出值
     */
    Value internalExecute(Value input);
}
