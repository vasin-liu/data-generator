/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import org.gensokyo.data.event.OnDoneListener;
import org.gensokyo.data.event.OnExceptionListener;
import org.gensokyo.data.stage.Stage;
import org.gensokyo.data.value.Value;

/**
 * 流水线接口定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/19 , Version 1.0.0
 */
public interface Pipeline {

    /**
     * 添加一个阶段到流水线中
     *
     * @param stage 阶段对象
     * @return 流水线对象
     */
    Pipeline next(Stage stage);

    /**
     * 执行流水线
     *
     * @param input 输入值
     * @return 输出值
     */
    Value execute(Value input);

    /**
     * 注册完成事件监听器
     *
     * @param listener 监听器
     */
    default Pipeline onDone(OnDoneListener listener) {
        return this;
    }

    /**
     * 注册异常事件监听器
     *
     * @param listener 监听器
     */
    default Pipeline onError(OnExceptionListener listener) {
        return this;
    }
}
