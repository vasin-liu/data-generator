/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

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

    Pipeline next(Stage stage);

    Value execute(Value input);
}
