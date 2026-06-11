/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.context;

import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.generator.GeneratorVO;

/**
 * 生成器上下文
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/18 , Version 1.0.0
 */
public record GeneratorContext<T extends GeneratorVO>(TemplateVO template,
                                                      T generator) {
}
