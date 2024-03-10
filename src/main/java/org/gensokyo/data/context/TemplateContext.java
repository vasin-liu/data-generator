/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.context;

import org.gensokyo.data.po.TemplatePO;
import org.gensokyo.data.value.Value;

import java.io.Serializable;

/**
 * 数据生成上下文对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public record TemplateContext(TemplatePO template, Value dataset) implements Serializable {

}
