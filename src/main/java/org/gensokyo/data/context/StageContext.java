/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.context;

import org.gensokyo.data.po.*;
import org.gensokyo.data.po.stage.StagePO;

/**
 * 阶段上下文
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public record StageContext<T extends StagePO>(TemplatePO template,
                                              FieldPO field,
                                              T stage) {
}
