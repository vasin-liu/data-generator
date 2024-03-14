/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.stage;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.convert.Converter;
import org.gensokyo.data.convert.StringConverter;

/**
 * 数据转换阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
public class ConvertStagePO extends StagePO {

    private Class<? extends Converter> converterType = StringConverter.class;
}
