/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.value.Value;

/**
 * 工厂接口定义
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/19 , Version 1.0.0
 */
public interface PipelineFactory {

    /**
     * 启动流水线
     */
    Value startup(final TemplateContext ctx);

    /**
     * 关闭流水线
     */
    void shutdown();
}
